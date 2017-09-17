## Calls Assistant

Приложение для отображения уведомления о звонках и SMS поступающих на телефон на экране планшета.

> Под *телефоном* и *планшетом* понимаются любые android-устройства - разделение для удобства.

> **Замечание** Для закрытия *уведомления* нажмите на область фотографии/номера телефона.

> **Замечание** При нажатии на кнопку `Ответить` будет создан только Intent
`kg.calls.assistant.call.answer`, т.к. "легального" программного способа ответить на звонок нет. Но
с помощью Intent'а можно реализовать например с помощью [Tasker](https://play.google.com/store/apps/details?id=net.dinglisch.android.taskerm)
или [Automate](https://play.google.com/store/apps/details?id=com.llamalab.automate).

<img src="https://github.com/delletenebre/CallsAssistant/raw/master/apk/screenshots/phone-call.png" height="320"> <img src="https://github.com/delletenebre/CallsAssistant/raw/master/apk/screenshots/tablet-call-notification.png" height="320">

## Разрешения
Для устройства *телефон* достаточно предоставить разрешения:
* **Контакты** - для отображения в *уведомлении* имени и фотографии.
* **Звонки** - для определения состояния звонков (ожидание, звонит, разговор, пропущенный).
* **Отправка SMS** - для передачи SMS с указанным Вами текстом, при получении команды от *планшета*.
<img src="https://github.com/delletenebre/CallsAssistant/raw/master/apk/screenshots/phone-permissions.png" height="320">

Для устройства *планшет* достаточно предоставить разрешения:
* **Уведомления** - для отображения уведомления.
* **Местоположение** - для передачи текущих координат (будут переданы в SMS).
<img src="https://github.com/delletenebre/CallsAssistant/raw/master/apk/screenshots/tablet-permissions.png" height="320">

## Как работает
При входящем/исходям звонке/SMS *телефон* формирует блок информации: номер телефона,
фотография и имя (если данный номер есть в Контактах), включенные для отображения кнопки, - и
передаёт на *планшет*.

При получении информации, *планшет* показывает окно поверх интерфейса системы.

### Настройка *телефона*
В первую очередь, необходимо указать тип соединения в разделе `Соединение`. Если тип соединения WiFi,
то необходимо выбрать `Порт` (значение порта должно совпадать с портом *планшета*).

В разделе `Клиент ("Телефон")`:
* если тип соединения Bluetooth - необходимо выбрать `Bluetooth-устройство` (заранее спаренное устройство *планшет*).
* если тип соединения WiFi - необходимо указать `IP "планшета"`.

Можно включить или отключить передачу информации на *планшет* о звонках и SMS.

В разделе `Сообщения` Вы указываете какие кнопки будут отображены в *уведомлении* на *планшете*, и текст отправляемых SMS. Таким образом, текста SMS настраиваются для каждого *телефона* отдельно.

<img src="https://github.com/delletenebre/CallsAssistant/raw/master/apk/screenshots/phone-settings-client.png" height="320">

### Настройка *планшета*
Для *планшета* необходимо указать тип соединения в разделе `Соединение`. Если тип соединения WiFi,
то необходимо выбрать `Порт` (значение порта должно совпадать с портом *телефона*).

В разделе `Сервер ("Планшет")` можно настроить стиль уведомления:
* `Размер элементов (dp)` - одним параметром изменяются размеры фотографии, имени/телефона абонента, кнопок.
* `Ширина уведомления (%)` - ширина *уведомления* в процентах от ширины экрана.

<img src="https://github.com/delletenebre/CallsAssistant/raw/master/apk/screenshots/tablet-ip.png" height="320">

## Intents
* `kg.calls.assistant.call.answer`
* `kg.calls.assistant.call.dismiss`
* `kg.calls.assistant.sms`
  * `phoneNumber` (String) - номер абонента для SMS.
  * `buttonNumber` (String) - номер нажатой кнопки для выбора текста сообщения
* `kg.calls.assistant.gps`
  * `phoneNumber` (String) - номер абонента для SMS.
  * `coordinates` (String) - текущие координаты в формате `latitude,longitute`
* `kg.calls.assistant.event`
  * `event` (String) - [response, call, sms]
  * `type` (String) / `event` == call || sms / - [incoming, outgoing]
  * `state` (String) / `event` == call / - [ringing, offhook, idle, missed]
  * `number` (String) - номер абонента
  * `name` (String) - имя абонента из Контактов
  * `photo` (String) - фотограифя абонента из Контактов
  * `message` (String) / `event` == sms / - сообщение входящего SMS
  * `buttons` (String) / `event` == call || sms / - содержит включенные в настройках *телефона*
  номера кнопок для отображения на *планшете*
  * `deviceAddress` (String) / `event` == response / - MAC-адрес bluetooth-устройства на которое нужно отправить команду
  при нажатиях на кнопки *уведомления*
