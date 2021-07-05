/*
   Copyright (c) 2017 Ahome' Innovation Technologies. All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ait.lienzo.client.core.shape.wires;

import java.util.Map;

import com.ait.lienzo.client.core.shape.IDirectionalMultiPointShape;
import com.ait.lienzo.client.core.shape.IPrimitive;
import com.ait.lienzo.client.core.shape.MultiPath;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.shared.core.types.ArrowEnd;
import com.ait.lienzo.shared.core.types.Direction;
import elemental2.dom.DomGlobal;

public class WiresConnection extends AbstractControlHandle {

    private final WiresConnector m_connector;

    private final IDirectionalMultiPointShape<?> m_line;

    private final MultiPath m_endPath;

    private ArrowEnd m_end;

    private boolean m_autoConnection;

    private double m_xOffset;

    private double m_yOffset;

    WiresMagnet m_magnet;

    public WiresConnection(WiresConnector connector, MultiPath endPath, ArrowEnd end) {
        m_connector = connector;
        m_line = connector.getLine();
        m_endPath = endPath;
        m_end = end;
        m_end = end;
    }

    public WiresConnection move(final double x, final double y) {
        DomGlobal.console.log("MOVE CONNECTION");
        IControlHandle handle;
        // Map<ControlHandleType, IControlHandleList> handles = m_connector.getPointHandles();
        IControlHandleList handles = m_connector.getLine().getControlHandles(ControlHandleStandardType.POINT).get(ControlHandleStandardType.POINT);
        //IControlHandleList handles = m_connector.getLine().getControlHandles(getType()).get(getType());
        if (m_end == ArrowEnd.HEAD) {
            handle = handles.getHandle(0);
        } else {
            handle = handles.getHandle(handles.size() - 1);
        }
        if (handle != null) {
            double rx = x + m_xOffset;
            double ry = y + m_yOffset;
            handle.setLocation(rx, ry);
            m_line.refresh();
            handle.getControl().setX(rx);
            handle.getControl().setY(ry);
        }
        if (m_line.getLayer() != null) {
            m_line.getLayer().batch();
        }
        m_connector.firePointsUpdated();
        return this;
    }

    public ArrowEnd getEnd() {
        return m_end;
    }

    public void setEnd(ArrowEnd end) {
        m_end = end;
    }

    public IDirectionalMultiPointShape<?> getLine() {
        return m_line;
    }

    public MultiPath getEndPath() {
        return m_endPath;
    }

    public WiresConnector getConnector() {
        return m_connector;
    }

    public boolean isAutoConnection() {
        return m_autoConnection;
    }

    public void setAutoConnection(boolean m_autoConnection) {
        this.m_autoConnection = m_autoConnection;
    }

    public double getXOffset() {
        return m_xOffset;
    }

    public void setXOffset(double m_xOffset) {
        this.m_xOffset = m_xOffset;
    }

    public double getYOffset() {
        return m_yOffset;
    }

    public void setYOffset(double m_yOffset) {
        this.m_yOffset = m_yOffset;
    }

    public WiresConnection setMagnet(final WiresMagnet magnet) {
        if (m_magnet != null) {
            m_magnet.removeHandle(this);
        }

        if (magnet != null) {
            DomGlobal.console.log("SETTING MAGNET");
            magnet.addHandle(this);

            Point2D absLoc = magnet.getControl().getComputedLocation();

            move(absLoc.getX() + m_xOffset, absLoc.getY() + m_yOffset);

            if (m_end == ArrowEnd.TAIL) {
                m_line.setTailDirection(magnet.getDirection());
            } else {
                m_line.setHeadDirection(magnet.getDirection());
            }
        } else {
            if (m_end == ArrowEnd.TAIL) {
                m_line.setTailDirection(Direction.NONE);
            } else {
                m_line.setHeadDirection(Direction.NONE);
            }
        }

        m_magnet = magnet;

        // The Line is only draggable if both Connections are unconnected
        m_connector.setDraggable();

        return this;
    }

    public Point2D getPoint() {
        // TODO: Check this...
        return (m_end == ArrowEnd.HEAD) ? m_line.getPoint2DArray().get(0) : m_line.getPoint2DArray().get(m_line.getPoint2DArray().size() - 1);
    }

    public void setPoint(Point2D point) {
        // TODO
    }

    public WiresMagnet getMagnet() {
        return m_magnet;
    }

    public WiresConnection getOppositeConnection() {
        return this == m_connector.getHeadConnection() ? m_connector.getTailConnection() : m_connector.getHeadConnection();
    }

    public boolean isSpecialConnection() {
        return isSpecialConnection(m_autoConnection,
                                   null != m_magnet ? m_magnet.getIndex() : null);
    }

    public static boolean isSpecialConnection(boolean auto,
                                              Integer magnet) {
        return auto || magnet != null && magnet == 0;
    }

    @Override
    public IPrimitive<?> getControl() {
        if (m_end == ArrowEnd.HEAD) {
            return m_connector.getPointHandles().getHandle(0).getControl();
        } else {
            return m_connector.getPointHandles().getHandle(m_connector.getPointHandles().size() - 1).getControl();
        }
    }

    @Override
    public ControlHandleType getType() {
        return ControlHandleStandardType.HANDLE;
    }

    @Override
    public void destroy() {
        super.destroy();
        setMagnet(null);
    }
}
