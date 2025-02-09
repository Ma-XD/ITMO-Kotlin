# Задание 6. Library

## Общая идея

Вы разрабатываете приложение для управления библиотекой в университете.

В распоряжении библиотеки есть книги ([`Book`](src/main/kotlin/library/api/Book.kt)), каждая книга имеет уникальный идентификатор, название, автора, год выпуска, описание: текстовое и список жанров.

У библиотеки есть посетители ([`Users`](src/main/kotlin/library/api/User.kt)), каждый посетитель имеет уникальный идентификатор, адрес электронной почты и имя.
Посетители могут брать на время (не больше чем на 7 дней) одну или несколько книг для домашнего чтения.


## Часть 1. Library Storage
По исторически сложившимся причинам данные библиотеки хранятся в двух файлах разного формата:
* Каталог книг хранится в файле `books.xml` (такой формат хранения информации о книгах достался нам от предыдущих разработчиков);
* Информация о пользователях и выданных книгах хранится в файле `state.json`.

Каталог книг имеет следующий формат.

```xml
<BookCatalog>
    <Book id="1" title="1984" author="George Orwell" year="1949">
        <Description description="You know what is" genres="dystopian,political fiction,science fiction"/>
    </Book>
    <!-- more books -->
</BookCatalog>
```

Остальная информация, как следует из названия, должна сохранятся в файл в формате __json__. Пример файла `state.json` приведен ниже.

```json
{
  "users": [
    { "id": "1", "email": "kbats@itmo.ru", "name": "kbats" }
  ],
  "borrowedBooks": [
    { "bookId": "1", "userId": "1", "returnDeadline": "2024-12-18T23:20:04.063266700Z" },
    { "bookId": "2", "userId": "1", "returnDeadline": "2024-11-19T23:20:04.063266700Z" }
  ]
}
```
 
Реализуйте  методы в объекте [`LibrarySerializer`](src/main/kotlin/library/data/LibrarySerializer.kt).
Для сериализация воспользуйтесь библиотекой [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) и [pdvrieze/xmlutil](https://github.com/pdvrieze/xmlutil).
Для настройки сериализации вам разрешается расставить необходимые аннотации `kotlinx.serialization` в уже существующих классах в пакете `api`.

Сделайте, чтобы каталог книг ([`BookCatalog`](src/main/kotlin/library/api/BookCatalog.kt)) и ([`LibraryState`](src/main/kotlin/library/api/LibraryState.kt)) представлялись в текстовом формате. Сериализованное представление должно быть отформатировано.

Реализуйте объявленные методы в объекте [`LibrarySerializer`](src/main/kotlin/library/data/LibrarySerializer.kt) так, чтобы
* Преобразовывать каталог книг ([`BookCatalog`](src/main/kotlin/library/api/BookCatalog.kt)) в формат xml и обратно;
* Преобразовывать информацию о посетителях и выданных книг ([`LibraryState`](src/main/kotlin/library/api/LibraryState.kt)) в формат json и обратно.

Решение можно протестировать с помощью тестов в классе [`LibrarySerializerTest`](src/test/kotlin/library/LibrarySerializerTest.kt) или через команду `gradlew test --tests "library.LibrarySerializerTest"`.

## Часть 2. Library Storage
Создайте класс [`FileLibraryStorage`](src/main/kotlin/library/data/FileLibraryStorage.kt), который реализует некоторую "базу данных" библиотеки, сохраняя все данные после каждого изменения в текстовые файлы `books.xml` и `state.json`.

В интерфейсе должны быть реализованы следующие методы:
* `allBooks(): List<Book>` - список всех книг, хранящихся в библиотеке;
* `allowedBooks(): List<Book>` - список книг, которые могут взять читатели в данный момент;
* `borrowedBooksInfo(): List<BorrowedBook>` - информация о книгах, которые читатели вяли для чтения;
* `createUser(email: String, name: String): User` - зарегистрировать посетителя библиотеки с адресом электронной почты `email` и именем `name` (в этом методе для посетителя должен генерироваться идентификатор, например _UUID_);
* `findUser(userId): User` - найти посетителя библиотеки по его идентификатору;
* `borrowBook(bookId: String, userId: String): Instant` - выдать посетителю с идентификатором `userId` кингу с идентификатором `bookId` на __7__ дней (этот метод должен возвращать дату, до которой посетитель должен вернуть книгу);
* `returnBook(bookId: String, userId: String)` - забрать у посетителя с идентификатором `userId` кингу с идентификатором `bookId`.

Решение можно протестировать с помощью тестов в классе [`FileLibraryStorageTest`](src/test/kotlin/library/FileLibraryStorageTest.kt) или с помощью команды `gradlew test --tests "library.FileLibraryStorageTest"`.

## Часть 3. Library Application
Теперь реализуйте многопоточное приложение для управления библиотекой.
Состояние приложение должно поддерживаться с использованием уже разработанного [`FileLibraryStorage`](src/main/kotlin/library/data/FileLibraryStorage.kt).
Все методы нашего приложения должны корректно работать при параллельных обращениях так, чтобы общее состояние приложения изменялось консистентно. 

Чтобы добиться этой цели, познакомьтесь с [моделью акторов](actors.md) и примените ее для решения этого пункта задания.
По сути, в нашем случае все изменения, применяемые к [`FileLibraryStorage`](src/main/kotlin/library/data/FileLibraryStorage.kt), будут выполняться внутри одной корутины с последовательным чтением
и обработкой событий из flow.

Реализуйте следующие методы в [LibraryApplication](src/main/kotlin/library/LibraryApplication.kt):
* `allBooks(): List<Book>` - список всех книг, хранящихся в библиотеке;
* `allowedBooks(): List<Book>` - список всех книг, которые могут взять читатели;
* `borrowedBooksInfo(): List<BorrowedBook>` - информация о книгах, которые читатели вяли для чтения;
* `createUser(email: String, name: String): User` - зарегистрировать посетителя библиотеки с адресом электронной почты `email` и именем `name` (в этом методе для посетителя должен выбираться случайный идентификатор, например _UUID_);
* `findUser(userId): User` - найти посетителя библиотеки по его идентификатору
* `borrowBook(bookId: String, userId: String): Instant` - выдать посетителю с идентификатором `userId` кингу с идентификатором `bookId`;
* `returnBook(bookId: String, userId: String)` - забрать у посетителя с идентификатором `userId` кингу с идентификатором `bookId`;
* `sendOverdueBooksNotification()` - послать напоминание по электронной почте тем посетителям библиотеки, которые не сдали книги вовремя.
  Пример текста напоминания
```text
Dear, Konstantin!
You didn't return 10 books with expired return date. Please return them as soon as possible.
Best regards.
```

Решение можно проверить с помощью тестов, которые запускаются в интерфейсе Intellij Idea или через консоль `./gradlew test` (*Nix) или `.\gradlew test` (Windows).
