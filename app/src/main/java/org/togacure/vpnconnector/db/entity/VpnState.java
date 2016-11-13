package org.togacure.vpnconnector.db.entity;

import com.j256.ormlite.field.DatabaseField;

/**
 * Created by togacure on 11/13/2016.
 */

public class VpnState extends AbstractEntity {

    @DatabaseField(canBeNull=false)
    private Boolean running = new Boolean(false);

    @DatabaseField
    private String sessionName;

    public Boolean getRunning() {
        return running;
    }

    public void setRunning(Boolean v) {
        running = v;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String name) {
        sessionName = name;
    }
}
