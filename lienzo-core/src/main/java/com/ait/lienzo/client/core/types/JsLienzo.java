package com.ait.lienzo.client.core.types;

import com.ait.lienzo.client.core.Context2D;
import com.ait.lienzo.client.core.NativeContext2D;
import com.ait.lienzo.client.core.shape.ContainerNode;
import com.ait.lienzo.client.core.shape.IPrimitive;
import com.ait.lienzo.client.core.shape.Layer;
import com.ait.lienzo.client.core.shape.Viewport;
import com.ait.lienzo.client.core.shape.wires.WiresManager;
import com.ait.lienzo.client.core.shape.wires.WiresShape;
import com.ait.lienzo.client.core.shape.wires.types.JsWiresShape;
import com.ait.lienzo.client.widget.panel.LienzoPanel;
import com.ait.lienzo.tools.client.collection.NFastArrayList;
import elemental2.dom.HTMLCanvasElement;
import jsinterop.annotations.JsType;

@JsType
public class JsLienzo {

    LienzoPanel panel;
    Layer layer;
    // TODO: Static?
    JsLienzoEvents events;
    // TODO: Static?
    JsLienzoAnimations animations;
    // TODO: Static?
    JsLienzoLogger logger;

    public JsLienzo(LienzoPanel panel, Layer layer) {
        this.panel = panel;
        this.layer = layer;
        this.events = null;
    }

    public Layer getLayer() {
        return layer;
    }

    public HTMLCanvasElement getCanvas() {
        HTMLCanvasElement canvasElement = getLayer().getCanvasElement();
        return canvasElement;
    }

    public Viewport getViewport() {
        return getLayer().getViewport();
    }

    public NativeContext2D getNativeContent() {
        Context2D context = getLayer().getContext();
        NativeContext2D nativeContext = context.getNativeContext();
        return nativeContext;
    }

    public JsLienzoEvents events() {
        if (null == events) {
            events = new JsLienzoEvents(this);
        }
        return events;
    }

    public JsLienzoAnimations animations() {
        if (null == animations) {
            animations = new JsLienzoAnimations();
        }
        return animations;
    }

    public JsLienzoLogger log() {
        if (null == logger) {
            logger = new JsLienzoLogger(this);
        }
        return logger;
    }

    public int getPanelOffsetLeft() {
        int result = panel.getElement().offsetLeft;
        return result;
    }

    public int getPanelOffsetTop() {
        int result = panel.getElement().offsetTop;
        return result;
    }

    public void add(IPrimitive<?> shape) {
        getLayer().add(shape);
    }

    public void draw() {
        getLayer().draw();
    }

    public IPrimitive<?> getShape(String id) {
        return getShapeInContainer(id, getLayer());
    }

    @SuppressWarnings("all")
    private static IPrimitive<?> getShapeInContainer(String id, ContainerNode parent) {
        NFastArrayList<IPrimitive<?>> shapes = parent.getChildNodes();
        if (null != shapes) {
            for (IPrimitive<?> shape : shapes.asList()) {
                String shapeID = shape.getID();
                if (id.equals(shapeID)) {
                    return shape;
                }
                if (shape instanceof ContainerNode) {
                    IPrimitive<?> shape1 = getShapeInContainer(id, (ContainerNode) shape);
                    if (null != shape1) {
                        return shape1;
                    }
                }
            }
        }
        return null;
    }

    public WiresManager getWiresManager() {
        return WiresManager.get(getLayer());
    }

    public JsWiresShape getWiresShape(String id) {
        WiresShape[] shapes = getWiresManager().getShapes();
        for (WiresShape shape : shapes) {
            if (id.equals(shape.getID())) {
                return new JsWiresShape(shape);
            }
        }
        return null;
    }
}
