package org.togacure.vpnconnector.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import org.togacure.vpnconnector.db.entity.AbstractEntity;
import org.togacure.vpnconnector.db.entity.VpnConfig;
import org.togacure.vpnconnector.db.entity.VpnState;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by togacure on 11/12/2016.
 */

public class DBHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "database.db";
    private static final int DATABASE_VERSION = 1;

    private static DBHelper singleton;
    private static final Object lock = new Object();

    private final Map<Class<? extends AbstractEntity>, Dao<? extends AbstractEntity, Integer>> daoMap =
            new HashMap<>();

    public static final DBHelper init(Context context) {
        synchronized (lock) {
            if (singleton == null) {
                singleton = OpenHelperManager.getHelper(context, DBHelper.class);
            }
        }
        return singleton;
    }

    public static final void release() {
        synchronized (lock) {
            if (singleton == null)
                return;
            OpenHelperManager.releaseHelper();
            singleton = null;
        }
    }

    public static final DBHelper instance() {
        return singleton;
    }

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, VpnConfig.class);
            TableUtils.createTable(connectionSource, VpnState.class);
            saveOrUpdate(new VpnState());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            TableUtils.dropTable(connectionSource, VpnConfig.class, true);
            TableUtils.dropTable(connectionSource, VpnState.class, true);
            onCreate(db, connectionSource);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends AbstractEntity> List<T> getAll(final Class<T> klass) {
        final List<T> result = new ArrayList<>();

        try {
            final Dao<T, Integer> dao = getDaoInternal(klass);
            if (dao != null)
                result.addAll(dao.queryForAll());
        } catch (SQLException e) {

        }

        return result;
    }

    public List<String> getAllVpnConfigNames() {
        final List<String> result = new ArrayList<>();
        for (final VpnConfig cfg : getAll(VpnConfig.class)) {
            result.add(cfg.getName());
        }
        return result;
    }

    public VpnConfig getVpnConfig(final String name) {
        VpnConfig result = null;

        for (final VpnConfig cfg : getAll(VpnConfig.class)) {
            if (!cfg.getName().equals(name))
                continue;
            result = cfg;
            break;
        }

        return result;
    }

    public VpnState getState() {
        try {
            final Dao<VpnState, Integer> dao = getDaoInternal(VpnState.class);
            return dao.queryForId(1);
        } catch (SQLException e) {

        }
        return null;
    }

    public boolean stateRunning() {
        final VpnState state = getState();
        return state == null ? false : state.getRunning();
    }

    public void stateRunning(Boolean v) {
        if (v == null)
            return;
        final VpnState state = getState();
        if (state == null)
            return;
        state.setRunning(v);
        saveOrUpdate(state);
    }

    public String stateSessionName() {
        final VpnState state = getState();
        return state == null ? null : state.getSessionName();
    }

    public void stateSessionName(String name) {
        if (name == null)
            return;
        final VpnState state = getState();
        if (state == null)
            return;
        state.setSessionName(name);
        saveOrUpdate(state);
    }

    public <T  extends AbstractEntity> void saveOrUpdate(T obj) {
        try {
            final Dao<T, Integer> dao = (Dao<T, Integer>)getDaoInternal(obj.getClass());
            dao.createOrUpdate(obj);
        } catch (SQLException e) {

        }
    }

    private <T  extends AbstractEntity> Dao<T, Integer> getDaoInternal(Class<T> klass)  throws SQLException {
        Dao<T, Integer> result = (Dao<T, Integer>)daoMap.get(klass);
        if (result == null) {
            result = getDao(klass);
            daoMap.put(klass, result);
        }
        return result;
    }

}
