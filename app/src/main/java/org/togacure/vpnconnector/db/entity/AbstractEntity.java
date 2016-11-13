package org.togacure.vpnconnector.db.entity;

import com.j256.ormlite.field.DatabaseField;

/**
 * Created by togacure on 11/12/2016.
 */

public abstract class AbstractEntity implements IEntity {

    @DatabaseField(generatedId = true)
    private Integer id;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }
}
