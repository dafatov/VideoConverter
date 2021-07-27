import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class GUI extends JFrame {
    private Video video;
    private JButton processVideo;
    private JMenuItem version;
    JButton setVideo;
    Thread animation;
    Area area;
    Area areaSub;
    JPanel processPanel;
    List<Object[]> shapes;
    boolean animate = false;

    GUI() {
        super("Видеоредактор");
        shapes = new ArrayList<>();
        createGUI();
        listeners();
        generateLoader();
        while (!chooseFFMPEG()) {
        }
        if (!new File(Main.ffmpeg).exists()) System.exit(-5);
    }

    private boolean chooseFFMPEG() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileFilter(new FileNameExtensionFilter("ffmpeg.exe", "exe"));
        fileChooser.setDialogTitle("Выбор ffmpeg.exe");
        switch (fileChooser.showDialog(this, "Выбрать")) {
            case JFileChooser.APPROVE_OPTION:
                Main.ffmpeg = fileChooser.getSelectedFile().getAbsolutePath();
                return true;
            case JFileChooser.ERROR_OPTION:
                return false;
            case JFileChooser.CANCEL_OPTION:
            default:
                System.exit(-3);
        }
        return true;
    }

    private void createGUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
        leftPanel.setBackground(Color.WHITE);
        mainPanel.add(leftPanel, BorderLayout.CENTER);

        processPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D graphics2D = (Graphics2D) g;
                super.paintComponent(g);

                if (!shapes.isEmpty()) {
                    for (Object[] shape : shapes) {
                        graphics2D.setColor((Color) shape[2]);
                        if ((Integer) shape[1] == 0) graphics2D.fill((Shape) shape[0]);
                        else if ((Integer) shape[1] == 1) graphics2D.draw((Shape) shape[0]);
                        else if ((Integer) shape[1] == 2) {
                            graphics2D.setFont(new Font("Tahoma", Font.PLAIN, (Integer) shape[5]));
                            FontMetrics fontMetrics = graphics2D.getFontMetrics();
                            graphics2D.drawString((String) shape[0], (Integer) shape[3] - (fontMetrics.stringWidth((String) shape[0]) / 2),
                                    (Integer) shape[4] + fontMetrics.getHeight() / 4);
                        }
                    }
                }
            }
        };
        processPanel.setBorder(new EtchedBorder());
        processPanel.setBackground(Color.WHITE);
        processPanel.setLayout(null);
        leftPanel.add(processPanel, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setLayout(new FlowLayout());
        leftPanel.add(south, BorderLayout.SOUTH);

        setVideo = new JButton("Открыть видео");
        south.add(setVideo);

        processVideo = new JButton("Обработать видео");
        processVideo.setEnabled(false);
        south.add(processVideo);

        setContentPane(mainPanel);
        setJMenuBar(createJBar());
        setPreferredSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width / 6, Toolkit.getDefaultToolkit().getScreenSize().height / 3));
        pack();
        setVisible(true);
        setResizable(false);
    }

    private JMenuBar createJBar() {
        JMenuBar jMenuBar = new JMenuBar();
        JMenu help = new JMenu("Помощь");
        version = new JMenuItem("О программе");

        help.add(version);
        jMenuBar.add(help);
        return jMenuBar;
    }

    private void generateLoader() {
        area = new Area();
        areaSub = new Area(new Ellipse2D.Double((processPanel.getWidth() >> 1) - ((Math.min(processPanel.getHeight(), processPanel.getWidth())) >> 2),
                (processPanel.getHeight() >> 1) - ((Math.min(processPanel.getHeight(), processPanel.getWidth())) >> 2),
                (Math.min(processPanel.getHeight(), processPanel.getWidth())) >> 1,
                (Math.min(processPanel.getHeight(), processPanel.getWidth())) >> 1));
        areaSub.add(new Area(new Rectangle2D.Double(0, processPanel.getHeight() - 20, processPanel.getWidth(), processPanel.getHeight())));

        shapes.add(new Object[]{area, 0, new Color(new Random().nextInt(256), new Random().nextInt(256), new Random().nextInt(256))});
        shapes.add(new Object[]{"0%", 2, Color.BLACK, processPanel.getWidth() / 2, processPanel.getHeight() / 2, 50});
        shapes.add(new Object[]{"Ожидание видео на обработку...", 2, Color.BLACK, processPanel.getWidth() / 2, processPanel.getHeight() - 10, 14});

        processPanel.repaint();
        setAnimation();
    }

    void setAnimation() {
        animation = new Thread(() -> {
            while (animate) {
                Color color = new Color(new Random().nextInt(256), new Random().nextInt(256), new Random().nextInt(256));
                double r = Math.sqrt(Math.pow(processPanel.getWidth(), 2) + Math.pow(processPanel.getHeight(), 2))+16;
                double a = -Math.PI / 2;
                double da = 3.6 * Math.PI / 180;
                long delay = 15;//>=15;
                int[] xList = {processPanel.getWidth() >> 1,
                        processPanel.getWidth() >> 1,
                        0};
                int[] yList = {processPanel.getHeight() >> 1,
                        (int) (processPanel.getHeight() - r),
                        0};

                while (a <= 3 * Math.PI / 2) {
                    int x = (int) (r * Math.cos(a)) + processPanel.getWidth() >> 1;
                    int y = (int) (r * Math.sin(a)) + processPanel.getHeight() >> 1;

                    xList[xList.length - 1] = x;
                    yList[yList.length - 1] = y;

                    area.reset();
                    if (animate) {
                        area.add(new Area(new Polygon(xList, yList, xList.length)));
                        area.subtract(areaSub);
                    }
                    Object[] object = shapes.get(0);
                    shapes.set(0, new Object[]{area, object[1], color});
                    processPanel.repaint();

                    if (!animate) break;

                    if (a >= -Math.PI / 4 && xList.length == 3 ||
                            a + da >= Math.PI / 4 && xList.length == 4 ||
                            a >= 3 * Math.PI / 4 && xList.length == 5 ||
                            a + da >= 5 * Math.PI / 4 && xList.length == 6) {
                        int[] tmp = new int[xList.length + 1];
                        System.arraycopy(xList, 0, tmp, 0, xList.length - 1);
                        xList = tmp;
                        tmp = new int[yList.length + 1];
                        xList[xList.length - 2] = x;
                        System.arraycopy(yList, 0, tmp, 0, yList.length - 1);
                        yList = tmp;
                        yList[yList.length - 2] = y;
                    }
                    try {
                        a += da;
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void listeners() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (video != null && video.process != null) video.process.destroy();
                System.exit(0);
            }
        });

        setVideo.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setFileFilter(new FileNameExtensionFilter("Video (.mp4)", "mp4"));
            fileChooser.setDialogTitle("Выбор видео");
            switch (fileChooser.showDialog(this, "Выбрать")) {
                case JFileChooser.APPROVE_OPTION:
                    video = new Video(GUI.this, fileChooser.getSelectedFile().getAbsolutePath());
                    setVideo.setEnabled(false);
                    processVideo.setEnabled(true);
                    Object[] object = shapes.get(2);
                    shapes.set(2, new Object[]{fileChooser.getSelectedFile().getAbsolutePath(), object[1], object[2], object[3], object[4], object[5]});
                    processPanel.repaint();
                    break;
                case JFileChooser.CANCEL_OPTION:
                    break;
                case JFileChooser.ERROR_OPTION:
                    break;
                default:
                    System.exit(-1);
            }
        });

        processVideo.addActionListener(e -> {
            if (video != null) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setMultiSelectionEnabled(false);
                fileChooser.setDialogTitle("Выбор директории");
                switch (fileChooser.showDialog(this, "Выбрать")) {
                    case JFileChooser.APPROVE_OPTION:
                        processVideo.setEnabled(false);
                        video.tempPath = fileChooser.getSelectedFile().getAbsolutePath();
                        video.resultPath = fileChooser.getSelectedFile().getAbsolutePath();
                        new Thread(video, "Video").start();
                        break;
                    case JFileChooser.CANCEL_OPTION:
                        break;
                    case JFileChooser.ERROR_OPTION:
                        break;
                    default:
                        System.exit(-1);
                }
            }
        });

        version.addActionListener(e -> {
            JOptionPane.showMessageDialog(GUI.this,
                    new String[]{"Автор: Афатов Дмитрий В."},
                    "Версия 0.0.1",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }
}
