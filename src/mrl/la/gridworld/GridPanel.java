package mrl.la.gridworld;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.Random;

/**
 * User: roohi
 * Date: Oct 1, 2010
 * Time: 12:32:59 PM
 */
public class GridPanel extends JPanel {
    GridWorld gridWorld;
    boolean started = false;
    Random random = new Random(System.currentTimeMillis());
    int gridLen = 50;
    int offsetX = 10;
    int offsetY = 10;

    public GridPanel(LayoutManager layoutManager) {
        super(layoutManager);

    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        if (started) {

            int curX = random.nextInt(gridWorld.width);
            int curY = random.nextInt(gridWorld.height);
            int desX = random.nextInt(gridWorld.width);
            int desY = random.nextInt(gridWorld.height);
            java.util.List<GridState> gridList = gridWorld.learning(curX, curY, desX, desY, 10000);
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(1));

            int tempX = offsetX;
            int tempY = offsetY;
            for (int i = 0; i < gridWorld.height * gridWorld.width; i++) {
                if (i % gridWorld.width == 0 && i != 0) {
                    tempX = offsetX;
                    tempY += gridLen;
                }
                int x[] = {tempX, tempX + gridLen, tempX + gridLen, tempX};
                int y[] = {tempY, tempY, tempY + gridLen, tempY + gridLen};

                g2d.draw(new Polygon(x, y, 4));


                tempX += gridLen;
            }
            g2d.setStroke(new BasicStroke(2));
            int x, y;
            for (int i = 0; i < gridList.size(); i++) {
                GridState state = gridList.get(i);
                if (i == 0) {
                    drawCircle(g2d, state, Color.GREEN);
                    continue;
                }
                drawArrow(g2d, gridList.get(i - 1), state, Color.BLACK);
                if (i == gridList.size() - 1) {
                    drawCircle(g2d, state, Color.RED);

                }
            }

        }
    }

    private void drawArrow(Graphics2D g, GridState state1, GridState state2, Color color) {
        Color last = g.getColor();
        g.setColor(color);

        g.drawLine(offsetX + (state1.getX() * gridLen) + (gridLen / 2),
                offsetY + (state1.getY() * gridLen) + (gridLen / 2),
                offsetX + (state2.getX() * gridLen) + (gridLen / 2),
                offsetY + (state2.getY() * gridLen) + (gridLen / 2));

        g.setColor(last);
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public void drawCircle(Graphics2D g, GridState state, Color color) {
        Color last = g.getColor();
        g.setColor(color);
        g.draw(new Ellipse2D.Float((gridLen / 4) + offsetX + (state.getX() * gridLen), (gridLen / 4) + offsetY + (state.getY() * gridLen), gridLen / 2, gridLen / 2));
        g.setColor(last);
    }

    public GridWorld getGridWorld() {
        return gridWorld;
    }

    public void setGridWorld(GridWorld gridWorld) {
        this.gridWorld = gridWorld;
    }

    public int getGridLen() {
        return gridLen;
    }

    public void setGridLen(int gridLen) {
        this.gridLen = gridLen;
    }
}
