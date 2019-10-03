# Server
A concurrent server to handle accounts is built.  Any number of brokers are allowed to connect to the server and perform a number of actions described in a README.md. Each account consists of an account number, and a pair of floating point values. The first floating point value is the amount of Arian, and the second is the amount of Pres currently in the account.The server is to listen on port number 4242 on localhost.  The brokers will connect on that port.
# Commands
Open〈account no〉Open a new account with both Arian and Pres set to 0.
State Print  the  current  state  of  all  accounts  and  the  current  conversion  rate. 
Rate〈rate〉Set the conversion rate to the given rate. The rate in interpreted as how many units of Pres equal one unit of Arian. The rate is a floating point number. The rate should never be set to 0.\
Convert〈account no〉(〈a〉,〈p〉) Convert Arian to Pres and vice versa within an account.
Transfer〈account from〉〈account to〉(〈a〉,〈p〉) Move a Arian and p Pres from one account to another.

