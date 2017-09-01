## Calls Assistant

Приложение для отображения уведомления о звонках и SMS поступающих на телефон на экране планшета.

> Под *телефоном* и *планшетом* понимаются любые android-устройства - разделение для удобства.

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

> **Замечание** Для закрытия *уведомления* нажмите на область фотографии/номера телефона.

### Настройка *телефона*
У *телефона* в настройках `Уведомление` > `Bluetooth-устройство` необходимо выбрать заранее спаренное устройство *планшет*.
Так же для *телефона* в разделе настроек `Уведомление` можно включить или отключить передачу информации на *планшет* о звонках и SMS.

<img src="https://github.com/delletenebre/CallsAssistant/raw/master/apk/screenshots/phone-settings-notification.png" height="320">

В разделе `Сообщения` Вы указываете какие кнопки будут отображены в *уведомлении* на *планшете*, и текст отправляемых SMS. Таким образом, текста SMS настраиваются для каждого *телефона* отдельно.

<img src="https://github.com/delletenebre/CallsAssistant/raw/master/apk/screenshots/phone-settings-messages.png" height="320">

### Настройка *планшета*
Для *планшета* можно настроить только размеры *уведомления* в разделе `Уведомление` > Стиль:
* `Размер элементов (dp)` - одним параметром изменяются размеры фотографии, имени/телефона абонента, кнопок.
* `Ширина уведомления (%)` - ширина *уведомления* в процентах от ширины экрана.

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
