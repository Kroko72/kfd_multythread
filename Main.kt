import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock


sealed class Transaction
class DepositTransaction(val clientId: Int, val amount: Double) : Transaction()
class WithdrawTransaction(val clientId: Int, val amount: Double) : Transaction()
class ExchangeCurrencyTransaction(val clientId: Int, val toCurrency: String) : Transaction()
class TransferTransaction(val clientId: Int, val receiverId: Int, val amount: Double) : Transaction()


class Client(
    val id: Int,
    var balance: Double,
    var currency: String
){
    val lock = ReentrantLock()
}


class Cashier(val id: Int, val bank: Bank): Thread() {

    //Запускаем поток
    override fun run() {
        while (true) {
            //Берём транзакцию
            val transaction = bank.transactionQueue.take()
            processTransaction(transaction)
        }
    }

    private fun processTransaction(transaction: Transaction) {
        when (transaction) {
            //Обрабатываем в соответсвии с запросом
            is DepositTransaction -> deposit(transaction.clientId, transaction.amount)
            is WithdrawTransaction -> withdraw(transaction.clientId, transaction.amount)
            is ExchangeCurrencyTransaction -> exchangeCurrency(transaction.clientId, transaction.toCurrency);
            is TransferTransaction -> transferTransaction(transaction.clientId, transaction.receiverId, transaction.amount);
        }
    }

    //Добавление
    private fun deposit(clientId: Int, amount: Double) {
        bank.clients[clientId]?.let {
            synchronized(it.lock){
                sleep(2000)
                bank.transactionQueue.offer(DepositTransaction(clientId, amount))
            }
        }
    }

    //Списание
    private fun withdraw(clientId: Int, amount: Double) {
        bank.clients[clientId]?.let {
            synchronized(it.lock){
                bank.transactionQueue.offer(WithdrawTransaction(clientId, amount))
            }
        }
    }

    private fun exchangeCurrency(clientId: Int, toCurrency: String) {
        bank.clients[clientId]?.let {
            synchronized(it.lock){
                bank.transactionQueue.offer(ExchangeCurrencyTransaction(clientId, toCurrency));
            }
        }
    }

    private fun transferTransaction(clientId: Int, receiverId: Int, amount: Double) {
        bank.clients[clientId]?.let {
            synchronized(it.lock) {
                bank.transactionQueue.offer(TransferTransaction(clientId, receiverId, amount));
            }
        }
    }
}


class Bank {
    val clients = ConcurrentHashMap<Int, Client>()
    val cashiers = ArrayList<Cashier>()
    val exchangeRates = ConcurrentHashMap<String, Double>()
    val transactionQueue = LinkedBlockingQueue<Transaction>() // Добавляем очередь транзакций

    private val observers = mutableListOf<Observer>()

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    private fun notifyObservers(message: String) {
        observers.forEach {
            it.update(message)
        }
    }

    //Обмен
    fun exchangeCurrency(clientId: Int, fromCurrency: String, toCurrency: String) {
        val client = clients[clientId]
        if (client != null && exchangeRates.containsKey(fromCurrency) && exchangeRates.containsKey(toCurrency)) {
            val rate = exchangeRates[toCurrency]!! / exchangeRates[fromCurrency]!!
            val newBalance = client.balance * rate

            client.balance = newBalance;
            client.currency = toCurrency

            val message = "Exchange: successful! Client $clientId exchanged to $newBalance $toCurrency"
            notifyObservers(message)
        } else {
            val message = "Exchange: failed! Client $clientId"
            notifyObservers(message)
        }
    }

    //Отправка денег
    fun transferFunds(senderId: Int, receiverId: Int, amount: Double) {
        val sender = clients[senderId]
        val receiver = clients[receiverId]

        if (sender != null && receiver != null && sender.balance >= amount) {
            val rate = exchangeRates[sender.currency]!! / exchangeRates[receiver.currency]!!

            sender.balance -= amount
            receiver.balance += amount * rate

            val message = "Funds transferred: Client $senderId transferred $amount to Client $receiverId"
            notifyObservers(message)
        } else {
            val message = "Funds transfer failed from Client $senderId to Client $receiverId"
            notifyObservers(message)
        }
    }

    //Внесение денег
    fun deposit(clientId: Int, amount: Double) {
        val client = clients[clientId]
        if (client != null && amount > 0) {
            client.balance += amount
            val message = "Deposit: Client $clientId deposited $amount ${client.currency}"
            notifyObservers(message)
        } else {
            val message = "Deposit failed for Client $clientId"
            notifyObservers(message)
        }
    }

    //Списывание
    fun withdraw(clientId: Int, amount: Double) {
        val client = clients[clientId]
        if (client != null && amount > 0 && client.balance >= amount) {
            client.balance -= amount
            val message = "Withdrawal: Client $clientId withdrew $amount ${client.currency}"
            notifyObservers(message)
        } else {
            val message = "Withdrawal failed for Client $clientId"
            notifyObservers(message)
        }
    }

    init {
        // Запускаем потоки-кассы для обработки очереди
        for (i in 1..5) {
            val cashier = Cashier(i, this)
            cashiers.add(cashier)
            cashier.start()
        }

        // Инициализируем курсы валют
        exchangeRates["USD"] = 1.0
        exchangeRates["EUR"] = 0.9

        // Запускаем поток для автоматического обновления курсов валют
        val executor = ScheduledThreadPoolExecutor(1)
        executor.scheduleAtFixedRate({
            exchangeRates["EUR"] = exchangeRates["EUR"]!! + getRandomExchangeRate()
        }, 0, 1, TimeUnit.SECONDS)
    }

    //Генерация рандмоных значений для изменения курса относительно доллара
    private fun getRandomExchangeRate(): Double{
        val add = (-20..20).random();
        return add / 100.0;
    }
}


class Logger: Observer{
    override fun update(message: String){
        println("Log: $message")
    }
}


interface Observer{
    fun update(message: String)
}


fun main(args: Array<String>) {
    val bank = Bank()
    val logger = Logger()

    // Регистрация логгера в банке
    bank.addObserver(logger)

    // Добавление клиентов
    bank.clients[1] = Client(1, 300.0, "USD")
    bank.clients[2] = Client(2, 500.0, "EUR")

    // Пример
    bank.exchangeCurrency(1, "USD", "EUR")
    bank.deposit(1, 500.0);
    bank.withdraw(1, 100.0)
    bank.transferFunds(1, 2, 300.0)
}