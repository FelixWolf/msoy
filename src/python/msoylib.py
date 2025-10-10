#!/usr/bin/env python3
import asyncio
import struct
import uuid
import hashlib
import aio_pika
from aiohttp_xmlrpc.client import ServerProxy


class MsoyMessaging:
    exchange = "whirled"

    def __init__(self, host, port, username, password, vhost="/"):
        self.host = host
        self.port = port
        self.username = username
        self.password = password
        self.vhost = vhost
        self.conn = None
        self.channel = None
        self.pendingFutures = {}

    async def connect(self):
        if self.conn and not self.conn.is_closed:
            return

        self.conn = await aio_pika.connect_robust(
            host=self.host,
            port=self.port,
            virtualhost=self.vhost,
            login=self.username,
            password=self.password,
        )
        self.channel = await self.conn.channel()
        self.exchangeHandle = await self.channel.get_exchange(self.exchange)
        self.replyQueue = await self.channel.declare_queue("amq.rabbitmq.reply-to", exclusive=True)
        await self.replyQueue.consume(self.onResponse, no_ack=True)
    
    async def onResponse(self, message):
        if message.correlation_id not in self.pendingFutures:
            return
        
        self.pendingFutures[message.correlation_id].set_result(message.body)
    
    async def close(self):
        if self.conn and not self.conn.is_closed:
            await self.conn.close()

    async def publish(self, route, body, needReply=False, timeout=1.0):
        await self.connect()
        correlationID = str(uuid.uuid4())

        if not needReply:
            await self.exchangeHandle.publish(
                aio_pika.Message(
                    body=body,
                    correlation_id=correlationID,
                    content_type="application/octet-stream",
                    delivery_mode=aio_pika.DeliveryMode.NOT_PERSISTENT
                ),
                routing_key=route,
            )
            return None

        loop = asyncio.get_event_loop()
        future = loop.create_future()
        self.pendingFutures[correlationID] = future

        await self.channel.set_qos(prefetch_count=1)

        await self.exchangeHandle.publish(
            aio_pika.Message(
                body=body,
                reply_to="amq.rabbitmq.reply-to",
                correlation_id=correlationID,
                content_type="application/octet-stream",
                delivery_mode=aio_pika.DeliveryMode.NOT_PERSISTENT
            ),
            routing_key=route,
        )
        
        try:
            result = await asyncio.wait_for(future, timeout)
        except asyncio.TimeoutError:
            result = b""
        
        del self.pendingFutures[correlationID]
        return result


    # === Implementations ===
    async def subscriptionBilled(self, who, months):
        """
        Message indicating a subscription payment was processed.
        """
        if isinstance(who, str):
            who = who.encode()

        response = await self.publish(
            "whirled.money.subscriptionBilled",
            struct.pack(">i", len(who)) + who + struct.pack(">i", months),
            needReply=True,
        )
    
    async def subscriptionEnded(self, who):
        """
        Message indicating a subscription payment was processed.
        """
        if isinstance(who, str):
            who = who.encode()

        response = await self.publish(
            "whirled.money.subscriptionEnded",
            struct.pack(">i", len(who)) + who,
            needReply=True,
        )
        
    async def getBarCount(self, who):
        """
        Message to retrieve the number of bars for a particular user.
        """
        if isinstance(who, str):
            who = who.encode()

        response = await self.publish(
            "whirled.money.getBarCount",
            struct.pack(">i", len(who)) + who,
            needReply=True,
        )

        if len(response) == 4:
            (count,) = struct.unpack(">i", response)
            return count

        return 0

    async def barsBought(self, who, what, how):
        """
        Message indicating a user purchased some number of bars.
        """
        if isinstance(who, str):
            who = who.encode()

        if isinstance(how, str):
            how = how.encode()

        await self.publish(
            "whirled.money.barsBought",
            struct.pack(">i", len(who))
            + who
            + struct.pack(">ii", what, len(how))
            + how,
        )

class MsoyXMLRPC:
    def __init__(self, host):
        loop = asyncio.get_event_loop()
        self.client = ServerProxy(host, loop=loop)
    
    async def authUser(self, username, password):
        hashed_password = hashlib.md5(password.encode('utf-8')).hexdigest()
        print(await self.client.user.authUser(username, hashed_password))

    async def authUserPermaName(self, username, password):
        hashed_password = hashlib.md5(password.encode('utf-8')).hexdigest()
        print(await self.client.user.authUserForWiki(username, hashed_password))

