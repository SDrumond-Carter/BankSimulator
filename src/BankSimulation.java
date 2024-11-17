/* Name: Samuel Matos Drumond
Course: CNT 4714 Fall 2024
Assignment title:
Project 2 – Synchronized/Cooperating Threads – A Banking Simulation
Due Date: September 29, 2024
*/

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

class BankAccount {
    private int balance = 0;
    private final ReentrantLock lock = new ReentrantLock();

    
    public void deposit(int amount, int transactionNumber, String agent) {
        lock.lock();
        try {
            balance += amount;
            System.out.printf("Agent deposits $%d into account (+)\n\n", amount);
            System.out.printf("New balance is $%d (Transaction #%d)\n\n", balance, transactionNumber);

            balance += amount;
            System.out.println(agent + " deposited $" + amount + ", New Balance: $" + balance + ", Transaction #" + transactionNumber);
            if (amount > 450) {
                flagTransaction(agent, amount, transactionNumber, "Deposit");
            }
        } finally {
            lock.unlock();
        }
    }

    public void withdraw(int amount, int transactionNumber, String agent) throws InterruptedException {
        lock.lock();
        try {
            while (balance < amount) {
                System.out.printf("Agent attempts to withdraw $%d from %s (******)\n\n", amount);
                System.out.printf("WITHDRAWAL BLOCKED - INSUFFICIENT FUNDS!!! Balance only $%d\n\n", balance);
            }

            balance -= amount;
            System.out.printf("Agent withdraws $%d from account (-)\n\n", amount);
            System.out.printf("Current balance is $%d (Transaction #%d)\n\n", balance, transactionNumber);


            if (balance >= amount) {
                balance -= amount;
                System.out.println(agent + " withdrew $" + amount + ", New Balance: $" + balance + ", Transaction #" + transactionNumber);
                if (amount > 90) {
                    flagTransaction(agent, amount, transactionNumber, "Withdrawal");
                }
            } else {
                System.out.println(agent + " tried to withdraw $" + amount + " but insufficient funds. Current Balance: $" + balance);
            }
        } finally {
            lock.unlock();
        }
    }

    public int getBalance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }

    private void flagTransaction(String agent, int amount, int transactionNumber, String type) {
        try (FileWriter writer = new FileWriter("transactions.csv", true)) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            writer.write(String.format("%s, %d, %s, %s, %s\n", now.format(formatter), transactionNumber, type, amount, agent));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class DepositorAgent implements Runnable {
    private final BankAccount[] accounts;
    private final Random rand = new Random();
    private static int transactionNumber = 1;

    public DepositorAgent(BankAccount[] accounts) {
        this.accounts = accounts;
    }

    @Override
    public void run() {
        while (true) {
            int accountIndex = rand.nextInt(accounts.length);
            int amount = rand.nextInt(600) + 1;
            accounts[accountIndex].deposit(amount, getNextTransactionNumber(), Thread.currentThread().getName());
            sleepRandom(2000); // Random sleep up to 2 seconds
        }
    }

    private static synchronized int getNextTransactionNumber() {
        return transactionNumber++;
    }

    private void sleepRandom(int maxSleep) {
        try {
            Thread.sleep(rand.nextInt(maxSleep));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class WithdrawalAgent implements Runnable {
    private final BankAccount[] accounts;
    private final Random rand = new Random();
    private static int transactionNumber = 1;

    public WithdrawalAgent(BankAccount[] accounts) {
        this.accounts = accounts;
    }

    @Override
    public void run() {
        while (true) {
            int accountIndex = rand.nextInt(accounts.length);
            int amount = rand.nextInt(99) + 1;
            try {
                accounts[accountIndex].withdraw(amount, getNextTransactionNumber(), Thread.currentThread().getName());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sleepRandom(1000); // Random sleep up to 1 second
        }
    }

    private static synchronized int getNextTransactionNumber() {
        return transactionNumber++;
    }

    private void sleepRandom(int maxSleep) {
        try {
            Thread.sleep(rand.nextInt(maxSleep));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class TransferAgent implements Runnable {
    private final BankAccount[] accounts;
    private final Random rand = new Random();
    private static int transactionNumber = 1;

    public TransferAgent(BankAccount[] accounts) {
        this.accounts = accounts;
    }

    @Override
    public void run() {
        while (true) {
            int fromAccountIndex = rand.nextInt(accounts.length);
            int toAccountIndex = rand.nextInt(accounts.length);
            while (toAccountIndex == fromAccountIndex) {
                toAccountIndex = rand.nextInt(accounts.length);
            }

            int amount = rand.nextInt(99) + 1;
            try {
                // Perform atomic transfer: withdraw from one account, deposit into another
                accounts[fromAccountIndex].withdraw(amount, getNextTransactionNumber(), Thread.currentThread().getName());
                accounts[toAccountIndex].deposit(amount, getNextTransactionNumber(), Thread.currentThread().getName());
            } catch (InterruptedException e) {
                System.out.println("Transfer interrupted: " + e.getMessage());
                Thread.currentThread().interrupt(); // Re-interrupt the thread
            }
            sleepRandom(3000); // Random sleep up to 3 seconds
        }
    }

    private static synchronized int getNextTransactionNumber() {
        return transactionNumber++;
    }

    private void sleepRandom(int maxSleep) {
        try {
            Thread.sleep(rand.nextInt(maxSleep));
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }
}

class InternalAuditAgent implements Runnable {
    private final BankAccount[] accounts;

    public InternalAuditAgent(BankAccount[] accounts) {
        this.accounts = accounts;
    }

    @Override
    public void run() {
        while (true) {
            int totalBalance = 0;
            for (BankAccount account : accounts) {
                totalBalance += account.getBalance();
            }
            
            System.out.println(Thread.currentThread().getName() + " audited total balance: $" + totalBalance);
            System.out.println("Internal Bank Audit Complete.\n\n");
            sleepRandom(5000); // Random sleep up to 5 seconds
        }
    }

    private void sleepRandom(int maxSleep) {
        try {
            Thread.sleep(new Random().nextInt(maxSleep));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class TreasuryAgent implements Runnable {
    private final BankAccount[] accounts;

    public TreasuryAgent(BankAccount[] accounts) {
        this.accounts = accounts;
    }

    @Override
    public void run() {
        while (true) {
            int totalBalance = 0;
            for (BankAccount account : accounts) {
                totalBalance += account.getBalance();
            }
            System.out.println(Thread.currentThread().getName() + " US Treasury audited total balance: $" + totalBalance);
            System.out.println("************************************************************************\n\n");
            sleepRandom(10000); // Random sleep up to 10 seconds
        }
    }

    private void sleepRandom(int maxSleep) {
        try {
            Thread.sleep(new Random().nextInt(maxSleep));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

public class BankSimulation {
    public static void main(String[] args) {
        BankAccount[] accounts = new BankAccount[2]; // Two bank accounts
        for (int i = 0; i < accounts.length; i++) {
            accounts[i] = new BankAccount(); // Each account starts with $0
        }

        ExecutorService executor = Executors.newFixedThreadPool(19);

        // Create 5 depositor agents
        for (int i = 0; i < 5; i++) {
            executor.execute(new DepositorAgent(accounts));
        }

        // Create 10 withdrawal agents
        for (int i = 0; i < 10; i++) {
            executor.execute(new WithdrawalAgent(accounts));
        }

        // Create 2 transfer agents
        executor.execute(new TransferAgent(accounts));
        executor.execute(new TransferAgent(accounts));

        // Create 1 internal audit agent
        executor.execute(new InternalAuditAgent(accounts));

        // Create 1 treasury agent
        executor.execute(new TreasuryAgent(accounts));

        executor.shutdown(); // This will not stop threads, as they run infinitely
    }
}
