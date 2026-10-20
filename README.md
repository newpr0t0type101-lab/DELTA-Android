# DELTA — изучение языка C на Android 🐙

DELTA — учебное Android-приложение на Java + WebView с офлайн-курсом,
маскотом Окто, очками XP, сериями и практическими заданиями.

## Сборка APK без Android Studio

1. Открой вкладку **Actions** репозитория.
2. Открой **DELTA Android APK** и нажми **Run workflow**.
3. Дождись успешного Build installable debug APK.
4. В разделе Artifacts скачай **DELTA-Android-debug-APK** и извлеки app-debug.apk.
5. На телефоне разреши установку APK из выбранного браузера/файлового менеджера.

Workflow автоматически запускается также при push в main.

Для сборки проекта вручную: `gradle -p android :app:assembleDebug`
(нужны JDK 17, Android SDK 35 и Gradle 8.10.2).

В комплекте 34 темы и 306 упражнений по C, на основе справочника Microsoft Learn:
https://learn.microsoft.com/en-us/cpp/c-language/c-language-reference

Учебные темы C++ (try/catch, классы) отделены от ISO C.
Компилятор GCC и общий онлайн-рейтинг требуют отдельный backend;
без сервера работают уроки, задания, XP и локальный прогресс.
