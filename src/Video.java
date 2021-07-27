import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Objects;

class Video implements Runnable {
    private GUI gui;
    private int duration;
    private int frameRate;
    private int durationNew;
    private String videoPath;
    String tempPath;
    Process process;
    String resultPath;

    Video(GUI gui, String videoPath) {
        this.gui = gui;
        this.videoPath = videoPath;
    }

    boolean changeAxises(String tmpDir, String resultDir) {
        File tmpDirFile = new File(tmpDir + "\\tmp");
        File frames = new File(tmpDir + "\\tmp\\frames");
        File framesNew = new File(tmpDir + "\\tmp\\framesNew");
        File resultDirFile = new File(resultDir);

        if (frameRate <= 0) return false;
        //if (frameRate*duration > Toolkit.getDefaultToolkit().getScreenSize().width) return false;
        if (!tmpDirFile.exists() && !tmpDirFile.mkdirs()) return false;
        if (!frames.exists() && !frames.mkdirs()) return false;
        if (!framesNew.exists() && !framesNew.mkdirs()) return false;
        if (Objects.requireNonNull(frames.listFiles()).length != 0) return false;
        if (Objects.requireNonNull(framesNew.listFiles()).length != 0) return false;
        if (!resultDirFile.exists() && !resultDirFile.mkdirs()) return false;

        if (videoToFrames(tmpDir) != 0) return false;
        if (!process(tmpDir)) return false;
        if (framesToVideo(tmpDir, resultDir) != 0) return false;
        Object[] object = gui.shapes.get(2);
        gui.shapes.set(2, new Object[]{"Удаляем оставшиеся временные файлы", object[1], object[2], object[3], object[4], object[5]});
        gui.processPanel.repaint();
        deleteDirectory(tmpDirFile);
        object = gui.shapes.get(1);
        gui.shapes.set(1, new Object[]{"0%", object[1], object[2], object[3], object[4], object[5]});
        object = gui.shapes.get(2);
        gui.shapes.set(2, new Object[]{"Ожидание видео на обработку...", object[1], object[2], object[3], object[4], object[5]});
        gui.processPanel.repaint();
        return true;
    }

    private void getParametres() {
        ProcessBuilder processBuilder = new ProcessBuilder(Main.ffmpeg, "-i", videoPath);
        try {
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
            InputStream std = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(std);
            BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                //FrameRate
                if (line.matches("(.)*fps(.)*")) {
                    String[] infos = line.split(",");
                    for (String info : infos) {
                        if (info.matches("(.)*fps(.)*")) {
                            frameRate = Math.round(Float.parseFloat(info.split(" ")[1]));
                            break;
                        }
                    }
                }
                //Duration
                if (line.matches("(.)*Duration(.)*")) {
                    duration = 0;
                    String[] infos = line.split(",");
                    infos = infos[0].split(" ");
                    infos = infos[3].split(":");
                    for (int i = 0; i < infos.length; i++) {
                        duration += Math.pow(60, i) * Math.round(Float.parseFloat(infos[infos.length - i - 1]));
                    }
                }
                if (frameRate != 0 && duration != 0) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteDirectory(File directory) {
        if (!directory.exists()) return;
        if (directory.isDirectory()) {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }

    private int videoToFrames(String tmpDir) {
        long framesDone;
        ProcessBuilder processBuilder = new ProcessBuilder(Main.ffmpeg, "-i", videoPath, "-y", tmpDir + "\\tmp\\frames\\image%d.png");
        try {
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
            InputStream std = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(std);
            BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                //System.out.println(line);
                if (line.matches("(.)*frame=(.)*")) {
                    String[] infos = line.replaceAll("( ){2,}", " ").split(" ");
                    framesDone = Long.parseLong(infos[1]);
                    int percent = Integer.parseInt(((String) gui.shapes.get(1)[0]).replaceAll("%", ""));
                    Object[] object = gui.shapes.get(2);
                    gui.shapes.set(2, new Object[]{"Преобразуем видео в кадры", object[1], object[2], object[3], object[4], object[5]});
                    if ((100 * framesDone) / (duration * frameRate) != percent) {
                        object = gui.shapes.get(1);
                        gui.shapes.set(1, new Object[]{((100 * framesDone) / (duration * frameRate) + "%"), object[1], object[2], object[3], object[4], object[5]});
                        gui.processPanel.repaint();
                    }
                }
            }
            return process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private int framesToVideo(String tmpDir, String resultDir) {
        long framesDone;
        ProcessBuilder processBuilder = new ProcessBuilder(Main.ffmpeg, "-r", String.valueOf(frameRate), "-i", tmpDir + "\\tmp\\framesNew\\image%d.png", "-y", resultDir + "\\result.mp4");
        try {
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            InputStream std = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(std);
            BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                if (line.matches("(.)*frame=(.)*")) {
                    String[] infos = line.replaceAll("( ){2,}", " ").split(" ");
                    framesDone = Long.parseLong(infos[1]);
                    int percent = Integer.parseInt(((String) gui.shapes.get(1)[0]).replaceAll("%", ""));
                    Object[] object = gui.shapes.get(2);
                    gui.shapes.set(2, new Object[]{"Преобразуем кадры в видео", object[1], object[2], object[3], object[4], object[5]});
                    if ((100 * framesDone) / (durationNew) != percent) {
                        object = gui.shapes.get(1);
                        gui.shapes.set(1, new Object[]{((100 * framesDone) / (durationNew) + "%"), object[1], object[2], object[3], object[4], object[5]});
                        gui.processPanel.repaint();
                    }
                }
            }

            return process.waitFor();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private boolean process(String tmpDir) {
        BufferedImage[] bufferedImages = new BufferedImage[Objects.requireNonNull(new File(tmpDir + "\\tmp\\frames").listFiles()).length];

        try {
            bufferedImages[0] = ImageIO.read(new File(tmpDir + "\\tmp\\frames\\image1.png"));
            for (int i = 2; i <= bufferedImages.length; i++) {
                bufferedImages[i - 1] = ImageIO.read(new File(tmpDir + "\\tmp\\frames\\image" + i + ".png"));
                if (bufferedImages[0].getWidth() != bufferedImages[i - 1].getWidth() && bufferedImages[0].getHeight() != bufferedImages[i - 1].getHeight()) {
                    return false;
                }
                int percent = Integer.parseInt(((String) gui.shapes.get(1)[0]).replaceAll("%", ""));
                Object[] object = gui.shapes.get(2);
                gui.shapes.set(2, new Object[]{"Читаем кадры в память", object[1], object[2], object[3], object[4], object[5]});
                if ((100 * i) / (bufferedImages.length) != percent) {
                    object = gui.shapes.get(1);
                    gui.shapes.set(1, new Object[]{((100 * i) / (bufferedImages.length) + "%"), object[1], object[2], object[3], object[4], object[5]});
                    gui.processPanel.repaint();
                }
            }
            durationNew = bufferedImages[0].getWidth();
            Object[] object = gui.shapes.get(2);
            gui.shapes.set(2, new Object[]{"Удаляем считанные кадры", object[1], object[2], object[3], object[4], object[5]});
            gui.processPanel.repaint();
            deleteDirectory(new File(tmpDir + "\\tmp\\frames"));
            for (int i = 0; i < bufferedImages[0].getWidth(); i++) {
                BufferedImage bufferedImage = new BufferedImage(bufferedImages.length, bufferedImages[0].getHeight(), BufferedImage.TYPE_INT_RGB);
                for (int j = 0; j < bufferedImages.length; j++) {
                    for (int k = 0; k < bufferedImages[0].getHeight(); k++) {
                        bufferedImage.setRGB(j, k, bufferedImages[j].getRGB(i, k));
                    }
                }
                ImageIO.write(bufferedImage, "png", new File(tmpDir + "\\tmp\\framesNew\\image" + (i + 1) + ".png"));
                int percent = Integer.parseInt(((String) gui.shapes.get(1)[0]).replaceAll("%", ""));
                object = gui.shapes.get(2);
                gui.shapes.set(2, new Object[]{"Преобразуем кадры", object[1], object[2], object[3], object[4], object[5]});
                if ((100 * i) / (bufferedImages[0].getWidth() - 1) != percent) {
                    object = gui.shapes.get(1);
                    gui.shapes.set(1, new Object[]{((100 * i) / (bufferedImages[0].getWidth() - 1) + "%"), object[1], object[2], object[3], object[4], object[5]});
                    gui.processPanel.repaint();
                }
            }
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public void run() {
        gui.animate = true;
        gui.setAnimation();
        gui.animation.start();

        getParametres();
        boolean sucess = changeAxises(tempPath, resultPath);
        gui.animate = false;
        if (!sucess)
            JOptionPane.showMessageDialog(gui, new String[]{"Что-то пошло не так",
                            "Эх... хотел бы я знать что,",
                            "но я не гадалка:)"},
                    "Ошибочка",
                    JOptionPane.ERROR_MESSAGE);
        gui.setVideo.setEnabled(true);
    }
}
