package com.hsts.client.login;

import java.util.ArrayList;

/**
 * Manages the number of login attempts and blocking the user with max failed attempt for t time.
 * Thread 1: Manages counting the failed attempts and blocks users with max failed attempts.
 * Thread 2: Checks if a user trying to login is blocked before allowing access.
 */
public class LoginManager {

    private final int maxFailedAttempts; // n - max failed attempts
    private final int blockDuration;     // t - block duration in seconds

    // An arrayList that saves the info(emails) of Users that are blocked because of too many failed attempts
    private final ArrayList<String> blockedUsers = new ArrayList<>();

    // An arrayList that saves each user's email and their failed attempt count together
    private final ArrayList<UserAttempt> failedAttempts = new ArrayList<>();

    // An arrayList that saves the remaining block time for each blocked user
    private final ArrayList<UserBlockTime> blockTimes = new ArrayList<>();

    /**
     * Inner class that holds an email and its failed attempt count together.
     */
    private static class UserAttempt {
        private final String email;
        private int count;

        UserAttempt(String email, int count) {
            this.email = email;
            this.count = count;
        }

        public String getEmail() {
            return email;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    // Inner class that holds an email and its remaining block time together
    private static class UserBlockTime {
        private final String email;
        private int remainingSeconds;

        UserBlockTime(String email, int remainingSeconds) {
            this.email = email;
            this.remainingSeconds = remainingSeconds;
        }

        public String getEmail() {
            return email;
        }

        public int getRemainingSeconds() {
            return remainingSeconds;
        }

        public void setRemainingSeconds(int remainingSeconds) {
            this.remainingSeconds = remainingSeconds;
        }
    }

    public LoginManager(int maxFailedAttempts, int blockDuration) {
        this.maxFailedAttempts = maxFailedAttempts;
        this.blockDuration = blockDuration;
    }

    // Gets the failed attempt count for a given email
    private int getFailedCount(String email) {
        for (UserAttempt ua : failedAttempts) {
            if (ua.getEmail().equals(email)) {
                return ua.getCount();
            }
        }
        return 0;
    }

    // Sets the failed attempt count for a given email for zero or just for increasing it
    private void setFailedCount(String email, int count) {
        for (UserAttempt ua : failedAttempts) {
            if (ua.getEmail().equals(email)) {
                ua.setCount(count);
                return;
            }
        }
        failedAttempts.add(new UserAttempt(email, count));
    }

    // Gets the remaining block time for a given email
    public int getRemainingTime(String email) {
        synchronized (this) {
            for (UserBlockTime ubt : blockTimes) {
                if (ubt.getEmail().equals(email)) {
                    return ubt.getRemainingSeconds();
                }
            }
        }
        return 0;
    }

    // Sets the remaining block time for a given email
    private void setRemainingTime(String email, int seconds) {
        for (UserBlockTime ubt : blockTimes) {
            if (ubt.getEmail().equals(email)) {
                ubt.setRemainingSeconds(seconds);
                return;
            }
        }
        blockTimes.add(new UserBlockTime(email, seconds));
    }

    // Removes the remaining block time for a given email
    private void removeRemainingTime(String email) {
        blockTimes.removeIf(ubt -> ubt.getEmail().equals(email));
    }

    // Checks if a user is currently blocked
    public boolean isBlocked(String email) {
        synchronized (this) {
            return blockedUsers.contains(email);
        }
    }

    /**
     * Thread 1: Records a failed login attempt for the given email.
     * If attempts reach maxFailedAttempts, blocks the user for blockDuration seconds.
     * onCountdown is called every second with the remaining seconds.
     * onUnblocked is called when the block duration ends.
     */
    public void recordFailedAttempt(String email, Runnable onError, CountdownCallback onCountdown, Runnable onUnblocked) {
        Thread thread1 = new Thread(() -> {
            boolean shouldBlock = false;

            // Only synchronize when reading/writing to shared data
            synchronized (this) {
                int attempts = getFailedCount(email) + 1;
                setFailedCount(email, attempts);

                if (attempts >= maxFailedAttempts) {
                    blockedUsers.add(email);
                    setFailedCount(email, 0);
                    setRemainingTime(email, blockDuration);
                    shouldBlock = true;
                }
            }

            if (shouldBlock) {
                // Run countdown outside synchronized so Thread 2 can check blockedUsers
                try {
                    int secondsLeft = blockDuration;
                    while (secondsLeft > 0) {
                        final int display = secondsLeft;
                        synchronized (this) {
                            setRemainingTime(email, display);
                        }
                        javafx.application.Platform.runLater(() ->
                                onCountdown.onTick(display)
                        );
                        Thread.sleep(1000L);
                        secondsLeft--;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Unblock the user
                synchronized (this) {
                    blockedUsers.remove(email);
                    removeRemainingTime(email);
                }

                // Notify UI that block is over
                javafx.application.Platform.runLater(onUnblocked);

            } else {
                // Not blocked yet, just show error
                javafx.application.Platform.runLater(onError);
            }
        });
        thread1.setName("FailedAttemptsThread");
        thread1.start();
    }

    /**
     * Thread 2: Checks if the user is blocked before allowing login.
     * Calls onAllowed if not blocked, onBlocked if blocked.
     */
    public void checkAndLogin(String email, Runnable onAllowed, Runnable onBlocked) {
        Thread thread2 = new Thread(() -> {
            // Only synchronize when reading from shared data
            boolean isBlocked;
            synchronized (this) {
                isBlocked = blockedUsers.contains(email);
            }
            if (isBlocked) {
                javafx.application.Platform.runLater(onBlocked);
            } else {
                javafx.application.Platform.runLater(onAllowed);
            }
        });
        thread2.setName("CheckBlockThread");
        thread2.start();
    }

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public int getBlockDuration() {
        return blockDuration;
    }

    // Functional interface for countdown callback
    public interface CountdownCallback {
        void onTick(int secondsLeft);
    }
}