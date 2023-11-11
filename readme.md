# Тут должна быть инструкция по использованию?
## Если да, то вот:

```kotlin
fun main(args: Array<String>){
    val bank = Bank()  // инициализация банка
    val logger = Logger()  // инициализация логгера

    // Регистрация логгера в банке
    bank.addObserver(logger)

    // Добавление клиентов
    bank.clients[1] = Client(1, 300.0, "USD")
    bank.clients[2] = Client(2, 500.0, "EUR")

    // Пример
    bank.exchangeCurrency(1, "USD", "EUR")  // перевод валют
    bank.deposit(1, 500.0);  // добавление на счёт
    bank.withdraw(1, 100.0)  // вывод со счёта
    bank.transferFunds(1, 2, 300.0)  // перевод на другой счёт
    
    /* Вывод для примера
    Log: Exchange: successful! Client 1 exchanged to 270.0 EUR
    Log: Deposit: Client 1 deposited 500.0 EUR
    Log: Withdrawal: Client 1 withdrew 100.0 EUR
    Log: Funds transferred: Client 1 transferred 300.0 to Client 2
    */
}
```
