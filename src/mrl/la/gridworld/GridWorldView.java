package mrl.la.gridworld;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * User: roohi
 * Date: Oct 1, 2010
 * Time: 12:07:04 PM
 */
public class GridWorldView extends JFrame {
    JPanel headerPanel;
    GridPanel centerPanel;
    JPanel footerPanel;
    GridWorld gridWorld;
    JTextField text1 = new JTextField(8);
    JTextField text2 = new JTextField(8);
    JTextField gridLen = new JTextField(8);

    private void initialize() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        JPanel mainPanel = new JPanel(new BorderLayout());
        headerPanel = new JPanel(new BorderLayout());
        centerPanel = new GridPanel(new BorderLayout());
        footerPanel = new JPanel(new BorderLayout());
        getContentPane().add(mainPanel);

        headerPanel.add(new JLabel("Grid World problem LA "));
        JButton button1 = new JButton("Start");
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                centerPanel.setGridWorld(
                        new GridWorld(Integer.valueOf(text1.getText()),
                                Integer.valueOf(text2.getText()),
                                0.1,
                                0.12));
                centerPanel.setGridLen(Integer.valueOf(gridLen.getText()));

                centerPanel.setStarted(true);
                centerPanel.repaint();

            }
        });
        footerPanel.add(button1, BorderLayout.EAST);

        JPanel dimension = new JPanel();
        dimension.add(text1);
        dimension.add(new JLabel(" X "));
        dimension.add(text2);
        dimension.add(new JLabel("   GridWidth: "));
        dimension.add(gridLen);
        footerPanel.add(dimension, BorderLayout.CENTER);
        text1.setText("5");
        text2.setText("5");
        gridLen.setText("50");

        JButton button2 = new JButton("Exit");
        button2.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.exit(0);
            }
        });

        footerPanel.add(button2, BorderLayout.WEST)
        ;
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

    }

    public GridWorldView() {
        initialize();
    }

    public static void main(String[] args) {
        GridWorldView view = new GridWorldView();
        view.setVisible(true);
    }
}
