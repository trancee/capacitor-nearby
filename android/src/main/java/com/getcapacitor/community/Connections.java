package com.getcapacitor.community;

import android.util.Log;
import com.getcapacitor.JSObject;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;

public class Connections extends NearbyPlugin {

    /**
     * States that the UI goes through.
     */
    public enum State {
        UNKNOWN,
        SEARCHING,
        CONNECTED
    }

    /**
     * The state of the app. As the app changes states, the UI will update and advertising/discovery
     * will start/stop.
     */
    private State mState = State.UNKNOWN;

    /**
     * The state has changed. I wonder what we'll be doing now.
     *
     * @param state The new state.
     */
    private void setState(State state) {
        if (mState == state) {
            Log.w(getLogTag(), "State set to " + state + " but already in that state");
            return;
        }

        Log.d(getLogTag(), "State set to " + state);
        State oldState = mState;
        mState = state;
        //onStateChanged(oldState, state);
    }

    /**
     * @return The current state.
     */
    private State getState() {
        return mState;
    }

    /**
     * Connection
     */

    @Override
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        // We accept the connection immediately.
        acceptConnection(endpoint);
    }

    @Override
    protected void onConnectionFailed(Endpoint endpoint) {
        //        // Let's try someone else.
        //        if (getState() == State.SEARCHING) {
        //            startDiscovering();
        //        }
    }

    /**
     * Endpoint
     */

    @Override
    protected void onEndpointConnected(Endpoint endpoint) {
        setState(State.CONNECTED);
    }

    @Override
    protected void onEndpointDisconnected(Endpoint endpoint) {
        setState(State.SEARCHING);
    }

    @Override
    protected void onEndpointFound(Endpoint endpoint) {
        if (getStrategy() != Strategy.P2P_CLUSTER) {
            // We found an advertiser!
            stopDiscovering();
        }

        JSObject jsData = new JSObject().put("uuid", endpoint.getId());
        notifyListeners("onFound", jsData);

        connectToEndpoint(endpoint);
    }

    @Override
    protected void onEndpointLost(String endpointId) {
        JSObject jsData = new JSObject().put("uuid", endpointId);
        notifyListeners("onLost", jsData);
    }

    /**
     * {@see Nearby#onReceive(Endpoint, Payload)}
     */
    @Override
    protected void onReceive(Endpoint endpoint, Payload payload) {
        //        if (payload.getType() == Payload.Type.STREAM) {
        //        }
    }
}
