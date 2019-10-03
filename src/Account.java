/**
 * This class handles any individual account.
 * Each account has an account number and amount of arian and pres associated with it.
 *
 * @author Eduard Zakarian
 */
public class Account implements Comparable<Account> {
    private Integer accNum; //Class variable because of its compareTo() method
    private float arian;
    private float pres;
    //A lock which will lead to a correct concurrent workflow of a program.
    private boolean lock;

    /**
     * Creates a new account with the specified account number and both currencies set to 0.
     *
     * @param accNum An account number t be associated with the account.
     */
    public Account(int accNum) {
        this.accNum = accNum;
        arian = 0;
        pres = 0;
        lock = false;
    }

    /**
     * Retrieves the account number.
     *
     * @return An integer containing the account number.
     */
    public Integer getAccNum() {
        return accNum;
    }

    /**
     * Retrieves the balance of arian.
     *
     * @return A float representing the balance of arian.
     */
    public synchronized float getArian() {
        return arian;
    }

    /**
     * Change the value of arian.
     *
     * @param arian A new value.
     */
    public synchronized void setArian(float arian) {
        this.arian = arian;
    }

    /**
     * Retrives the balance of pres.
     *
     * @return A float representing the balance of pres.
     */
    public synchronized float getPres() {
        return pres;
    }

    /**
     * Change the value of pres.
     *
     * @param pres A new value.
     */
    public synchronized void setPres(float pres) {
        this.pres = pres;
    }

    /**
     * Get the current state of accounts lock
     *
     * @return Returns true is the lock is currently acquired, false otherwise.
     */
    public boolean isLock() {
        return lock;
    }

    /**
     * Change the lock state of an account
     *
     * @param lock A new lock value, either True or False.
     */
    public void setLock(boolean lock) {
        this.lock = lock;
    }

    /**
     * Implements the Comparable interface in order to allow the sorting
     * of an Account array list.
     *
     * @param a The account to compare with the current one.
     * @return 0   if this = that,
     *         < 0 if this < that,
     *         > 0 if this > that.
     */
    @Override
    public int compareTo(Account a) {
        return this.getAccNum().compareTo(a.getAccNum());
    }
}