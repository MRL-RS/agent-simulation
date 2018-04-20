package mrl.mrlPersonal.viewer;

import rescuecore2.standard.entities.StandardEntity;

import java.awt.*;

/**
 * User: roohi
 * Date: May 7, 2010
 * Time: 11:02:52 AM
 */
public class StandardEntityToPaint {
    StandardEntity entity;
    Color color;

    public StandardEntityToPaint(StandardEntity entity, Color color) {
        this.entity = entity;
        this.color = color;
    }

    public StandardEntity getEntity() {
        return entity;
    }

    public void setEntity(StandardEntity entity) {
        this.entity = entity;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
