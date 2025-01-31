//DilaraCaglaBanko 202128201
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RabbitGame {

    private static final Random random = new Random();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter number of rabbits: ");
        int rabbitCount = scanner.nextInt();

        System.out.print("Enter number of boxes: ");
        int boxCount = scanner.nextInt();

        System.out.print("Enter carrot producing rate (X): ");
        int carrotRate = scanner.nextInt();

        System.out.print("Enter carrot timeout (Y): ");
        int carrotTimeout = scanner.nextInt();

        System.out.print("Enter rabbit jump delay (Z): ");
        int rabbitJumpDelay = scanner.nextInt();

        Game game = new Game(rabbitCount, boxCount, carrotRate, carrotTimeout, rabbitJumpDelay);
        game.start();
    }

    static class Game {
        private final int rabbitCount;
        private final int boxCount;
        private final int carrotRate;
        private final int carrotTimeout;
        private final int rabbitJumpDelay;

        private final Box[] boxes;
        private final List<Rabbit> rabbits;
        private final ScheduledExecutorService executor; //thread managemnt
        private final AtomicBoolean gameOver;

        public Game(int rabbitCount, int boxCount, int carrotRate, int carrotTimeout, int rabbitJumpDelay) {
            this.rabbitCount = rabbitCount;
            this.boxCount = boxCount;
            this.carrotRate = carrotRate;
            this.carrotTimeout = carrotTimeout;
            this.rabbitJumpDelay = rabbitJumpDelay;

            this.boxes = new Box[boxCount];
            for (int i = 0; i < boxCount; i++) {
                boxes[i] = new Box(i);
            }

            this.rabbits = new ArrayList<>();
            for (int i = 1; i <= rabbitCount; i++) {
                rabbits.add(new Rabbit("Rabbit" + i));
            }

            this.executor = Executors.newScheduledThreadPool(rabbitCount + 2); //thread management
            this.gameOver = new AtomicBoolean(false); // initial game state
        }

        public void start() {
            System.out.println("\n----The game starts!----\n");

            executor.scheduleAtFixedRate(this::placeCarrot, 0, carrotRate, TimeUnit.MILLISECONDS); // carrot placement process

            for (Rabbit rabbit : rabbits) {
                executor.submit(rabbit);
            }

            executor.scheduleAtFixedRate(this::checkGameOver, 0, 100, TimeUnit.MILLISECONDS); // the game's end is checked regularly
        }

        private void placeCarrot() {
            if (gameOver.get()) return;

            int boxIndex = random.nextInt(boxCount);
            if (boxes[boxIndex].placeCarrot()) {
                System.out.println("\n**PERSON PUTS CARROT IN BOX " + boxIndex + "**\n");
            }
        }

        private void checkGameOver() {
            if (gameOver.get()) return;

            boolean allFinished = rabbits.stream().allMatch(rabbit -> rabbit.getCurrentBox() == boxCount - 1);
            if (allFinished) {
                gameOver.set(true);
                endGame();
            }
        }

        private void endGame() {
            executor.shutdownNow();
            System.out.println("\n----SCORE----\n");
            for (Rabbit rabbit : rabbits) {
                System.out.println(rabbit.getName() + " has " + rabbit.getScore() + " points.");
            }
            System.out.println("\n----Game over!----");
        }

        class Box {
            private final int index;
            private final AtomicBoolean hasCarrot;

            public Box(int index) {
                this.index = index;
                this.hasCarrot = new AtomicBoolean(false);
            }

            public synchronized boolean placeCarrot() {
                if (hasCarrot.get()) return false;

                hasCarrot.set(true);
                executor.schedule(this::removeCarrot, carrotTimeout, TimeUnit.MILLISECONDS);
                return true;
            }

            public synchronized boolean eatCarrot() {
                if (hasCarrot.get()) {
                    hasCarrot.set(false);
                    return true;
                }
                return false;
            }

            private synchronized void removeCarrot() {
                if (hasCarrot.get() && !gameOver.get()) {
                    hasCarrot.set(false);
                    System.out.println("\n**CARROT in box " + index + " removed**\n");
                }
            }
        }

        class Rabbit implements Runnable {
            private final String name;
            private int currentBox;
            private int score;

            public Rabbit(String name) {
                this.name = name;
                this.currentBox = 0;
                this.score = 0;
            }

            public String getName() {
                return name;
            }

            public int getScore() {
                return score;
            }

            public int getCurrentBox() {
                return currentBox;
            }

            @Override
            public void run() {
                try {
                    Thread.sleep(random.nextInt(500)); // random start delay
                    //in the example, I determined a random start because the rabbits did not start at the same time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                while (!gameOver.get() && currentBox < boxCount - 1) {
                    try {
                        Thread.sleep(rabbitJumpDelay); 
                        currentBox++;
                        System.out.println(name + " jumps to box " + currentBox);

                        if (boxes[currentBox].eatCarrot()) {
                            score++;
                            System.out.println("\n***" + name + " EATS carrot in box " + currentBox + "***\n");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }
}
