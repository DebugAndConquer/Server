import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This program hosts a server. The aim is to allow multiple users to connect to it at the same time
 * and perform the operations described in assignment's specification document. The program should work
 * concurrently without losing any information or allowing any deadlock situations. If any incorrect
 * query is entered by a user, there should be a reasonable response from the server side to a user.
 *
 * @author Eduard Zakarian
 */


public class Server {
    //A regular expression to cover the correct input format (...) used in transfer and convert
    private static final String REGEX_VALUES = "\\(\\S+\\)";
    //Regular expressions to cover server commands and prevent bugs during the input
    private static final String REGEX_RATE = "(?i)[r][a][t][e][ ](.*?)";
    private static final String REGEX_OPEN = "(?i)[o][p][e][n][ ](.*?)";
    private static final String REGEX_STATE = "(?i)[s][t][a][t][e]";
    private static final String REGEX_TRANSFER = "(?i)[t][r][a][n][s][f][e][r][ ](.*?)";
    private static final String REGEX_CONVERT = "(?i)[c][o][n][v][e][r][t][ ](.*?)";
    private static final String REGEX_CLOSE = "(?i)[c][l][o][s][e]";
    //A set of monitors used to play a role in providing condition synchronisation and mutual exclusion
    private static final Object OPEN_MONITOR = new Object();
    private static final Object ACCOUNTS_MONITOR = new Object();
    private static final Object RATE_MONITOR = new Object();
    //An array list of all accounts created during the session
    private static ArrayList<Account> accList = new ArrayList<>();
    //Default value: 10
    private static float rate = 10;
    /*
     * I use these two locks below and a lock for each account to allow concurrent access to different commands
     * instead of blocking the whole system while the operation is being processed. Several monitors are used
     * for the same reason.
     */
    private static boolean openLock = false;
    private static boolean rateLock = false;


    /**
     * Runs the server. When a client connects, the server spawns a new thread to do
     * the servicing.
     */
    public static void main(String[] args) throws Exception {
        //Setting US locale to force using '.' as a decimal part separator instead of ','
        Locale.setDefault(new Locale("en", "US"));
        try (ServerSocket listener = new ServerSocket(4242)) {
            ExecutorService pool = Executors.newFixedThreadPool(1000);
            while (true) {
                pool.execute(new Talk(listener.accept()));
            }
        }
    }

    /**
     * This private class is responsible for concurrent operation of the program.
     * The code describes the behaviour of the server as well as robustness of all possible operations.
     */
    private static class Talk implements Runnable {
        private Socket socket;

        Talk(Socket socket) {
            this.socket = socket;
        }

        /**
         * Searches for the account in the account list.
         *
         * @param accNum A unique identity number of the account.
         * @return true if the account is found, false otherwise.
         */
        private static Boolean findAcc(int accNum) {
            boolean flag = false;
            for (Account a : accList) {
                if (accNum == a.getAccNum()) {
                    flag = true;
                }
            }
            return flag;
        }

        /**
         * Searches for an array list's index of the account with
         * the specified account number.
         *
         * @param accNum A unique identity number of the account.
         * @return An index of the account in the array list.
         */
        private static int getAccIndex(int accNum) {
            int index = -1; //Error Value
            for (int i = 0; i < accList.size(); i++) {
                if (accNum == accList.get(i).getAccNum()) {
                    index = i;
                }
            }
            return index;
        }

        /**
         * Checks the validness of an input and transfers money if no violations were found.
         *
         * @param line A line to be read from the console.
         * @param out  An interface to display text output to a user in the client.
         */
        private static void checkValidnessAndTransfer(Scanner line, PrintWriter out)
                throws InterruptedException {
            //The following checks the legitness of input and performs a transfer of funds
            int accFrom;
            int accTo;
            line.next();
            //The following if statements are to make sure sensible data is provided during the input
            if (line.hasNextInt()) {
                accFrom = line.nextInt();
                if (line.hasNextInt()) {
                    accTo = line.nextInt();
                    //Sender and receiver must be different accounts
                    if (accFrom != accTo) {
                        //Both accounts should exist to perform a transaction
                        if (findAcc(accFrom) && findAcc(accTo)) {
                            try {
                                String values = line.next();
                                //If the values are entered in a "(number,number)" format, then proceed
                                if (values.matches(REGEX_VALUES)) {
                                    String numericValues = getNumberString(values);
                                    //Creating a new Scanner to extract floats from numericValues string
                                    Scanner valueScan = new Scanner(numericValues);
                                    try {
                                        float arian = valueScan.nextFloat();
                                        float pres = valueScan.nextFloat();
                                        //Getting the indexes of account for which locks should be acquired
                                        int indexFrom = getAccIndex(accFrom);
                                        int indexTo = getAccIndex(accTo);
                                        valueScan.close();
                                        float newArianFrom = accList.get(indexFrom).getArian() - arian;
                                        float newArianTo = accList.get(indexTo).getArian() + arian;
                                        float newPresFrom = accList.get(indexFrom).getPres() - pres;
                                        float newPresTo = accList.get(indexTo).getPres() + pres;
                                        /*
                                         * Acquiring one account lock at the time. First one is used while funds
                                         * are being removed from the senders account and second one is used
                                         * while the funds are being added to a receivers account.
                                         */
                                        synchronized (ACCOUNTS_MONITOR) {
                                            while (accList.get(indexFrom).isLock()) {
                                                accList.get(indexFrom).wait();
                                            }
                                            accList.get(indexFrom).setLock(true);
                                            accList.get(indexFrom).setArian(newArianFrom);
                                            accList.get(indexFrom).setPres(newPresFrom);
                                            accList.get(indexFrom).setLock(false);

                                            while (accList.get(indexTo).isLock()) {
                                                accList.get(indexTo).wait();
                                            }
                                            accList.get(indexTo).setLock(true);
                                            accList.get(indexTo).setArian(newArianTo);
                                            accList.get(indexTo).setPres(newPresTo);
                                            accList.get(indexTo).setLock(false);
                                            ACCOUNTS_MONITOR.notify();
                                        }
                                        out.println("Transferred");
                                    } catch (NoSuchElementException e) {
                                        out.println("Values should be floating-point numbers!");
                                    }
                                } else {
                                    out.println("Wrong Format of values. The Correct Format is:" +
                                            " (number,number). Example: (3,4) or (8.567,9.1)");
                                }
                            } catch (NoSuchElementException e) {
                                out.println("Please provide an amount of money to transfer!");
                            }
                        } else {
                            out.println("One or both of the accounts does not exist in the system!");
                        }
                    } else {
                        out.println("You cannot transfer to the same account!");
                    }
                } else {
                    out.println("Second account number should be an integer!");
                }
            } else {
                out.println("First account number should be an integer!");
            }
        }

        /**
         * Checks the validness of an input and converts money if no violations were found.
         *
         * @param line A line to be read from the console.
         * @param out  An interface to display text output to a user in the client.
         */

        private static void checkValidnessAndConvert(Scanner line, PrintWriter out) throws InterruptedException {
            //The following happens when convert command is called
            line.next();
            //The following if statements are to make sure sensible data is provided during the input
            if (line.hasNextInt()) {
                int accNum = line.nextInt();
                if (findAcc(accNum)) {
                    try {
                        String values = line.next();
                        //If the values are entered in a "(number,number)" format, then proceed
                        if (values.matches(REGEX_VALUES)) {
                            String numericValues = getNumberString(values);
                            //Creating a new Scanner to extract floats from numericValues string
                            Scanner valueScan = new Scanner(numericValues);
                            try {
                                float arian = valueScan.nextFloat();
                                float pres = valueScan.nextFloat();
                                valueScan.close();
                                //Getting the index of an account for which a lock should be acquired
                                int index = getAccIndex(accNum);
                                float newArian = accList.get(index).getArian() - arian + pres / rate;
                                float newPres = accList.get(index).getPres() - pres + arian * rate;

                                //Locking the account while the transaction is in progress.
                                synchronized (ACCOUNTS_MONITOR) {
                                    while (accList.get(index).isLock()) {
                                        ACCOUNTS_MONITOR.wait();
                                    }
                                    accList.get(index).setLock(true);
                                    accList.get(index).setArian(newArian);
                                    accList.get(index).setPres(newPres);
                                    accList.get(index).setLock(false);
                                    ACCOUNTS_MONITOR.notify();
                                }
                                out.println("Converted");

                            } catch (NoSuchElementException e) {
                                out.println("Values should be floating-point numbers!");
                            }
                        } else {
                            out.println("Wrong Format of values. The Correct Format is:" +
                                    " (number,number). Example: (3,4) or (8.567,9.1)");
                        }
                    } catch (NoSuchElementException e) {
                        out.println("Please provide an amount of money to convert!");
                    }
                } else {
                    out.println("Account is not found on the server!");
                }
            } else {
                out.println("Account number should be an integer!");
            }
        }

        /**
         * Converts a string of the format "(number,number)" to a string of the format "number number".
         * It is needed  to successfully extract floats in the future operations.
         *
         * @param values An initial string.
         * @return A formatted string.
         */
        private static String getNumberString(String values) {
            String numericValues = values.replaceAll("\\(", "");
            numericValues = numericValues.replaceAll("\\)", "");
            numericValues = numericValues.replaceAll("[,]", " ");
            return numericValues;
        }

        /**
         * Checks for the valid input and changes the rate to a specified value.
         *
         * @param line A line to be read from the console.
         * @param out  An interface to display text output to a user in the client.
         */
        private static void changeRate(Scanner line, PrintWriter out) throws InterruptedException {
            //The following sets the rate to a specified value
            line.next();
            float r = rate; //Temporary rate
            try {
                r = line.nextFloat();
            } catch (InputMismatchException e) {
                out.println("Please enter a float value!");
            }
            if (r < 0) {
                out.println("Rate cannot be a negative number!");
            } else if (r == 0) {
                out.println("Rate cannot be set to 0. Please retry!");
            } else {
                //Acquiring a lock  to prevent other processes using rate.
                synchronized (RATE_MONITOR) {
                    while (rateLock) {
                        RATE_MONITOR.wait();
                    }
                    rateLock = true;
                    rate = r;
                    rateLock = false;
                    RATE_MONITOR.notify();
                }
                out.println("Rate changed");
            }
        }

        /**
         * Checks the validness of an input and creates a new account if no violations were found.
         *
         * @param line A line to be read from the console.
         * @param out  An interface to display text output to a user in the client.
         */
        private static void openAccount(Scanner line, PrintWriter out) throws InterruptedException {
            //The following creates a new account with the specified account number
            int accNumber;
            line.next();
            Account a;
            //Adding protection from non-integer values for acc number
            if (line.hasNextInt()) {
                accNumber = line.nextInt();
                a = new Account(accNumber);
                //Preventing clashes during the creation of the account using a locking mechanism
                synchronized (OPEN_MONITOR) {
                    while (openLock) {
                        OPEN_MONITOR.wait();
                    }
                    openLock = true;
                    //Do not allow the same account to be created more than once
                    if (findAcc(accNumber)) {
                        out.println("Cannot create a duplicate account!" + " (" + accNumber + ")");
                    } else {
                        accList.add(a);
                        out.println("Opened account" + " " + accNumber);
                    }
                    openLock = false;
                    OPEN_MONITOR.notify();
                }
            } else {
                out.println("Please provide an integer value for the account number!");
            }
        }

        /**
         * Prints the state of all accounts and a conversion rate to the client window.
         *
         * @param out An instance of PrintWriter that prints text in the client window.
         */
        private static void printState(PrintWriter out) throws InterruptedException {
            //Not allowing to modify any account info while state is being printed
            synchronized (ACCOUNTS_MONITOR) {
                Collections.sort(accList);
                for (int i = 0; i < accList.size(); i++) {
                    while (accList.get(i).isLock()) {
                        ACCOUNTS_MONITOR.wait();
                    }
                    accList.get(i).setLock(true);
                    out.println(accList.get(i).getAccNum() + ": " + "Arian " +
                            accList.get(i).getArian() + ", Pres " + accList.get(i).getPres());
                    accList.get(i).setLock(false);
                    ACCOUNTS_MONITOR.notify();
                }
            }
            //Not allowing to modify rate while rate is being printed
            synchronized (RATE_MONITOR) {
                while (rateLock) {
                    RATE_MONITOR.wait();
                }
                rateLock = true;
                out.println("Rate " + rate);
                rateLock = false;
                RATE_MONITOR.notify();
            }
        }

        /**
         * The following method describes the behaviour of the program for every connected client.
         * There may be several running clients at the same time.
         */
        public void run() {
            System.out.println("Connected: " + socket);
            try {
                Scanner in = new Scanner(socket.getInputStream());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                Scanner line = null;
                while (in.hasNextLine()) {
                    //Initialising the scanner for the current line to prevent '/n' error
                    String curLine = "";
                    if (in.hasNextLine()) {
                        curLine = in.nextLine();
                        line = new Scanner(curLine);
                    }
                    //The following block gathers a command from the client and performs the expected operation
                    if (curLine.matches(REGEX_RATE)) {
                        changeRate(line, out);
                    } else if (curLine.matches(REGEX_OPEN)) {
                        openAccount(line, out);
                    } else if (curLine.matches(REGEX_STATE)) {
                        printState(out);
                    } else if (curLine.matches(REGEX_TRANSFER)) {
                        checkValidnessAndTransfer(line, out);
                    } else if (curLine.matches(REGEX_CONVERT)) {
                        checkValidnessAndConvert(line, out);
                    } else if (curLine.matches(REGEX_CLOSE)) {
                        //EXTRA FEATURE: The following happens in case a user would want to disconnect
                        out.println("Have a nice day!");
                        socket.close();
                    } else {
                        out.println("Unsupported command or missing/unnecessary argument(s)!");
                    }
                    line.close();
                }
                in.close();
            } catch (Exception e) {
                System.out.println("Error:" + socket + " " + e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                }
                System.out.println("Closed: " + socket);
            }
        }
    }
}