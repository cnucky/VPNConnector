package org.togacure.vpnconnector.db.entity;

import com.j256.ormlite.field.DatabaseField;

/**
 * Created by togacure on 11/12/2016.
 */

public class VpnConfig extends AbstractEntity{

    @DatabaseField(canBeNull=false)
    private String name;

    @DatabaseField(canBeNull=false)
    private String address;

    @DatabaseField(canBeNull=false)
    private String route;

    @DatabaseField
    private String dns;

    @DatabaseField
    private String searchDomain;

    @DatabaseField
    private Integer mtu;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getDns() {
        return dns;
    }

    public void setDns(String dns) {
        this.dns = dns;
    }

    public String getSearchDomain() {
        return searchDomain;
    }

    public void setSearchDomain(String searchDomain) {
        this.searchDomain = searchDomain;
    }

    public Integer getMtu() {
        return mtu;
    }

    public void setMtu(Integer mtu) {
        this.mtu = mtu;
    }
}
