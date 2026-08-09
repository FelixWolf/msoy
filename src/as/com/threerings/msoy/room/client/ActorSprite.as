//
// $Id$

package com.threerings.msoy.room.client {

import com.threerings.util.DelayUtil;
import com.threerings.util.Log;
import com.threerings.util.Util;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.room.data.ActorInfo;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.util.NamedValueEvent;
import com.threerings.msoy.ui.MsoyMediaContainer;

/**
 * Handles sprites for actors (members and pets).
 */
public class ActorSprite extends OccupantSprite
{
    /**
     * Creates an actor sprite for the supplied actor.
     */
    public function ActorSprite (ctx :WorldContext, occInfo :ActorInfo, extraInfo :Object)
    {
        super(ctx, occInfo, extraInfo);
        Prefs.events.addEventListener(Prefs.PREF_SET, function (e:NamedValueEvent):void {
            try {
                if (e.name == Prefs.SHOW_MATURE) {
                    checkMatureAndBlock(getItemIdent());
                }
            } catch (err:Error) {}
        });
    }

    /**
     * Update the actor's state. Called by user code when it wants to change the actor's state.
     */
    public function setState (state :String) :void
    {
        var ctrl :RoomController = getController(true);
        if (ctrl != null && validateUserData(state, null)) {
            ctrl.setActorState(_ident, _occInfo.bodyOid, state);
        }
    }

    /**
     * Get the actor's current state.  Called by user code.
     */
    public function getState () :String
    {
        return getActorInfo().getState();
    }

    /**
     * Returns the actor info for this actor.
     */
    public function getActorInfo () :ActorInfo
    {
        return (_occInfo as ActorInfo);
    }

    /**
     * @return true if we're idle.
     */
    public function isIdle () :Boolean
    {
        return (_occInfo as ActorInfo).isIdle();
    }

    // from OccupantSprite
    override public function getDesc () :String
    {
        return "m.actor";
    }

    // from OccupantSprite
    override public function setOccupantInfo (newInfo :OccupantInfo, extraInfo :Object) :void
    {
        // note whether our state changed (we don't consider it a change if the old info was null,
        // as getting our initial state isn't a "change"); this is reason to have a special
        // state-changed dobj event
        var stateChanged :Boolean =
            (_occInfo != null && !Util.equals((_occInfo as ActorInfo).getState(),
                                              (newInfo as ActorInfo).getState()));

        super.setOccupantInfo(newInfo, extraInfo);

        // note our new item ident, our entity id in the room
        setItemIdent((newInfo is ActorInfo) ? (newInfo as ActorInfo).getItemIdent() : null);

        // if the state has changed, dispatch an event
        if (stateChanged) {
            callUserCode("stateSet_v1", getState());
        }
    }

    // from OccupantSprite
    override public function toString () :String
    {
        return "ActorSprite[" + _occInfo.username + " (oid=" + _occInfo.bodyOid + ")]";
    }

    /**
     * Update the actor's scene location. Called by our backend in response to a request from
     * usercode.
     */
    public function setLocationFromUser (x :Number, y :Number, z: Number, orient :Number) :void
    {
        var ctrl :RoomController = getController(true);
        if (ctrl != null) {
            ctrl.requestMove(_ident, new MsoyLocation(x, y, z, orient));
        }
    }

    /**
     * Update the actor's orientation.
     * Called by user code when it wants to change the actor's scene location.
     */
    public function setOrientationFromUser (orient :Number) :void
    {
        // TODO
        Log.getLog(this).debug("user-set orientation is currently TODO.");
    }

    public function setMoveSpeedFromUser (speed :Number) :void
    {
        if (!isNaN(speed)) {
            // don't worry, it'll be bounded by the minimum at the appropriate place
            _moveSpeed = speed;
        }
    }

    // from OccupantSprite
    override protected function configureDisplay (
        oldInfo :OccupantInfo, newInfo :OccupantInfo) :Boolean
    {
        // update the item ident
        const oldIdent :ItemIdent = getItemIdent();
        const newIdent :ItemIdent = ActorInfo(newInfo).getItemIdent();
        const identChanged :Boolean = !newIdent.equals(oldIdent);
        if (identChanged) {
            var view :RoomView = _sprite.parent as RoomView;
            if (view != null) {
                // pop down any popup
                view.getRoomController().clearEntityPopup(this);
                // indicate that the old entity has left, the new one arrives
                view.dispatchEntityLeft(oldIdent);
                DelayUtil.delayFrame(view.dispatchEntityLeft, [ newIdent ]);
            }

            // and set the new ident
            setItemIdent(newIdent);
        }

        // check the media
        const newMedia :MediaDesc = (newInfo as ActorInfo).getMedia();
        const mediaChanged :Boolean = !newMedia.equals(_sprite.getMediaDesc());
        if (!mediaChanged && !identChanged) {
            return false; // nothing changed, bail
        }
        if (!mediaChanged) {
            // if the media didn't change, but the ident did, we still need to reload the media
            // so force a change
            _sprite.setMediaDesc(null);
        }
        _sprite.setMediaDesc(newMedia);
        // check whether this actor's avatar item is mature and block if necessary
        try {
            var ident:ItemIdent = ActorInfo(newInfo).getItemIdent();
            checkMatureAndBlock(ident);
        } catch (e:Error) {}
        return true; // and indicate to callers that something changed
    }

    protected static var _matureCache2:Object = {};
    protected static var _pending2:Object = {};

    protected function checkMatureAndBlock (ident:ItemIdent) :void
    {
        if (ident == null) return;
        var key:String = ident.type + ":" + ident.itemId;
        var applyBlock:Function = function (isMature:Boolean) :void {
            try {
                var wantShow:Boolean = Prefs.getShowMature();
                var msc:MsoyMediaContainer = (_sprite as MsoyMediaContainer);
                if (msc == null) return;
                if (isMature && !wantShow) {
                    msc.setBlocked("bleep");
                } else {
                    msc.setBlocked(null);
                }
            } catch (e:Error) {}
        };
        if (_matureCache2.hasOwnProperty(key)) { applyBlock(Boolean(_matureCache2[key])); return; }
        if (_pending2.hasOwnProperty(key)) { _pending2[key].push(applyBlock); return; }
        _pending2[key] = [ applyBlock ];
        try {
            var svc:ItemService = _ctx.getClient().requireService(ItemService) as ItemService;
            svc.peepItem(ident, _ctx.resultListener(function (item:Object):void {
                var isMature:Boolean = false;
                try { isMature = (item as Item).mature; } catch (e:Error) { isMature = false; }
                _matureCache2[key] = isMature;
                var arr:Array = _pending2[key] as Array; delete _pending2[key];
                for each (var cb:Function in arr) { try { cb(isMature); } catch (ee:Error) {} }
            }));
        } catch (e:Error) {
            _matureCache2[key] = false;
            var arr2:Array = _pending2[key] as Array; delete _pending2[key];
            for each (var cb2:Function in arr2) { try { cb2(false); } catch (ee:Error) {} }
        }
    }

    // from OccupantSprite
    override protected function appearanceChanged () :void
    {
        var locArray :Array = [ _loc.x, _loc.y, _loc.z ];
        if (hasUserCode("appearanceChanged_v2")) {
            callUserCode("appearanceChanged_v2", locArray, _loc.orient, isMoving(), isIdle());
        } else {
            callUserCode("appearanceChanged_v1", locArray, _loc.orient, isMoving());
        }
    }

    // from EntitySprite
    override protected function createBackend () :EntityBackend
    {
        return new ActorBackend();
    }
}
}
