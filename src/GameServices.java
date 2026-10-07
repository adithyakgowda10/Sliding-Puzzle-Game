import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.Timer;

public final class GameServices {

    private GameServices() {
    }

static final class GameTimer {

    interface TimerListener {
        void onTimeTick(int totalSeconds, String formattedTime);
    }

    private int secondsElapsed;
    private final Timer swingTimer;
    private boolean running;
    private TimerListener listener;

    GameTimer() {
        swingTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                secondsElapsed++;
                notifyListener();
            }
        });
    }

    void setListener(TimerListener listener) {
        this.listener = listener;
    }

    void start() {
        if (!running) {
            swingTimer.start();
            running = true;
        }
    }

    void stop() {
        if (running) {
            swingTimer.stop();
            running = false;
        }
    }

    void reset() {
        stop();
        secondsElapsed = 0;
        notifyListener();
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onTimeTick(secondsElapsed, getFormattedTime());
        }
    }

    static String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    String getFormattedTime() {
        return formatTime(secondsElapsed);
    }

    int getSecondsElapsed() {
        return secondsElapsed;
    }

    boolean isRunning() {
        return running;
    }
}

static final class SoundManager {

    private static SoundManager instance;
    private volatile boolean soundEnabled = true;
    private static final float SAMPLE_RATE = 16000f;

    private SoundManager() {
    }

    static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    boolean isSoundEnabled() {
        return soundEnabled;
    }

    void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    void toggleSound() {
        soundEnabled = !soundEnabled;
    }

    void playMoveSound() {
        if (soundEnabled) {
            playToneAsync(520, 35, 0.25);
        }
    }

    void playShuffleSound() {
        if (!soundEnabled) {
            return;
        }
        playSequence(new int[] {400, 480, 560, 640}, new int[] {25, 25, 25, 25}, 0.15);
    }

    void playWinSound() {
        if (!soundEnabled) {
            return;
        }
        playSequence(new int[] {523, 659, 784, 1046}, new int[] {100, 100, 100, 280}, 0.35);
    }

    void playInvalidMoveSound() {
        if (soundEnabled) {
            playToneAsync(220, 40, 0.15);
        }
    }

    private void playSequence(int[] frequencies, int[] durations, double volume) {
        Thread audioThread = new Thread(() -> {
            for (int i = 0; i < frequencies.length && soundEnabled; i++) {
                playTone(frequencies[i], durations[i], volume);
                try {
                    Thread.sleep(40);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "puzzle-audio");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    private void playToneAsync(int frequencyHz, int durationMs, double volume) {
        Thread audioThread = new Thread(() -> playTone(frequencyHz, durationMs, volume), "puzzle-audio");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    private synchronized void playTone(int frequencyHz, int durationMs, double volume) {
        SourceDataLine line = null;
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();

            int sampleCount = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
            byte[] buffer = new byte[sampleCount];
            for (int i = 0; i < sampleCount; i++) {
                double angle = 2.0 * Math.PI * i / (SAMPLE_RATE / frequencyHz);
                double envelope = 1.0 - ((double) i / sampleCount);
                buffer[i] = (byte) (Math.sin(angle) * 127.0 * volume * envelope);
            }

            line.write(buffer, 0, buffer.length);
            line.drain();
        } catch (LineUnavailableException | IllegalArgumentException ex) {
            System.err.println("Unable to play puzzle sound: " + ex.getMessage());
        } finally {
            if (line != null) {
                line.close();
            }
        }
        }
    }
}
