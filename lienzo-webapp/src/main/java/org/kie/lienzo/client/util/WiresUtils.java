package org.kie.lienzo.client.util;

import com.ait.lienzo.client.core.shape.IDirectionalMultiPointShape;
import com.ait.lienzo.client.core.shape.MultiPath;
import com.ait.lienzo.client.core.shape.MultiPathDecorator;
import com.ait.lienzo.client.core.shape.OrthogonalGroupPolyLine;
import com.ait.lienzo.client.core.shape.OrthogonalMultipointPolyLine;
import com.ait.lienzo.client.core.shape.OrthogonalPolyLine;
import com.ait.lienzo.client.core.shape.VHPolyLine;
import com.ait.lienzo.client.core.shape.wires.MagnetManager;
import com.ait.lienzo.client.core.shape.wires.WiresConnector;
import com.ait.lienzo.client.core.shape.wires.WiresMagnet;
import com.ait.lienzo.client.core.shape.wires.WiresManager;
import com.ait.lienzo.client.core.types.Point2DArray;
import com.ait.lienzo.shared.core.types.Direction;
import elemental2.dom.DomGlobal;

public class WiresUtils {

    public static WiresConnector connect(MagnetManager.Magnets magnets0,
                                         int i0_1,
                                         MagnetManager.Magnets magnets1,
                                         int i1_1,
                                         WiresManager wiresManager,
                                         boolean orthogonalPolyline) {
        WiresMagnet m0_1 = magnets0.getMagnet(i0_1);
        WiresMagnet m1_1 = magnets1.getMagnet(i1_1);

        double x0, x1, y0, y1;

        MultiPath head = new MultiPath();
        head.M(15,
               20);
        head.L(0,
               20);
        head.L(15 / 2,
               0);
        head.Z();

        MultiPath tail = new MultiPath();
        tail.M(15,
               20);
        tail.L(0,
               20);
        tail.L(15 / 2,
               0);
        tail.Z();

        IDirectionalMultiPointShape<?> line;

        x0 = m0_1.getControl().getX();
        y0 = m0_1.getControl().getY();
        x1 = m1_1.getControl().getX();
        y1 = m1_1.getControl().getY();

        double ox0 = x0 + ((x1 - x0) / 2);
        double oy0 = y0 + ((y1 - y0) / 2);
        DomGlobal.console.log("POINTS [" + x0 + ", " + y0 + ", " + x1 + ", " + y1 + "]");
        if (orthogonalPolyline) {
            // line = createOrthogonalPolyline(x0, y0, x1, y1);
            line = createOrthogonalMultipointPolyline(x0, y0, x1, y1);
            /*line = createOrthogonalMultipointPolyline(ox0, oy0,
                                                      ox0, y0,
                                                      ox0, y1,
                                                      x1, y1);*/
        } else {
            line = createPolyline(ox0, oy0, x1, y1);
        }

        line.setHeadDirection(Direction.NONE);
        line.setTailDirection(Direction.NONE);

        line.setHeadOffset(head.getBoundingBox().getHeight());
        line.setTailOffset(tail.getBoundingBox().getHeight());
        line.setSelectionStrokeOffset(25);

        WiresConnector connector = new WiresConnector(m0_1,
                                                      m1_1,
                                                      line,
                                                      new MultiPathDecorator(head),
                                                      new MultiPathDecorator(tail));
        wiresManager.register(connector);

        head.setStrokeWidth(5).setStrokeColor("#0000CC");
        tail.setStrokeWidth(5).setStrokeColor("#0000CC");
        line.setStrokeWidth(5).setStrokeColor("#0000CC");

        return connector;
    }

    public static OrthogonalGroupPolyLine createOrthogonalGroupPolyline(final double... points) {
        return createOrthogonalGroupPolyline(Point2DArray.fromArrayOfDouble(points));
    }

    public static OrthogonalGroupPolyLine createOrthogonalGroupPolyline(final Point2DArray points) {
        return new OrthogonalGroupPolyLine().setPoint2DArray(points).setDraggable(true);
    }

    public static OrthogonalPolyLine createOrthogonalPolyline(final double... points) {
        return createOrthogonalPolyline(Point2DArray.fromArrayOfDouble(points));
    }

    public static OrthogonalPolyLine createOrthogonalPolyline(final Point2DArray points) {
        return new OrthogonalPolyLine(points).setCornerRadius(5).setDraggable(true);
    }

    public static OrthogonalMultipointPolyLine createOrthogonalMultipointPolyline(final double... points) {
        return createOrthogonalMultipointPolyline(Point2DArray.fromArrayOfDouble(points));
    }

    public static OrthogonalMultipointPolyLine createOrthogonalMultipointPolyline(final Point2DArray points) {
        return new OrthogonalMultipointPolyLine(points).setCornerRadius(5).setDraggable(true);
    }

    public static VHPolyLine createPolyline(final double... points) {
        return createPolyline(Point2DArray.fromArrayOfDouble(points));
    }

    public static VHPolyLine createPolyline(final Point2DArray points) {
        return new VHPolyLine(points).setCornerRadius(5).setDraggable(true);
    }
}
