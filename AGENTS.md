# Предпочтения Алиски для модов

Пользователь просит обращаться к ней «Алиска».

Для всех новых модов и изменений этого проекта по умолчанию готовить GitHub CI/CD:
- сборка и необходимые проверки на push и pull request;
- автоматическая публикация успешной сборки из основной ветки в GitHub Releases;
- постоянное имя JAR без версии, чтобы ссылка `/releases/latest/download/<mod-id>.jar` оставалась неизменной (для этого мода — `btr-world.jar`);
- SHA256 и исходники рядом с JAR;
- отдельные релизы с историей сборок; повторный запуск старого коммита не должен откатывать latest;
- явно отличать подготовленный workflow от реально запущенного на GitHub.

Применять этот стандарт к будущим модам, если Алиска не просит иначе. Для внешней публикации нужен конкретный репозиторий и доступ; не придумывать владельца или URL работающего релиза.

# Команды сборки и проверок

- Среда: Java 21, Fabric Loom 1.7.4.
- Запуск Gradle: `./gradlew` может не иметь прав на исполнение — вызывайте через `bash ./gradlew ...`.
- Сборка JAR: `bash ./gradlew build` (версия переопределяется параметром `-Pmod_version=<version>`, базовая версия в `gradle.properties`).
- Проверки логики мода в Gradle:
  - `bash ./gradlew check` — запускает проверки генетики и нотных мелодий.
  - `bash ./gradlew geneticsCheck` — отдельная проверка распределения потомства и поколений.
  - `bash ./gradlew musicCheck` — отдельная проверка диапазонов нот оригинальных мелодий.
  - Стандартная задача `test` возвращает `NO-SOURCE` (тесты вынесены в задачи `JavaExec`).
- Тесты Python:
  - Все тесты: `python3 -m unittest discover -s tests -v`
  - Отдельные модули: `python3 -m unittest tests/test_stages.py`, `python3 -m unittest tests/test_modrinth_release.py`.
- Валидация ресурсов и рецептов:
  - `python3 scripts/validate_assets.py` — проверяет 630+ JSON, пути текстур в атласе, координаты геометрии `[-16, 32]`, отсутствие конфликтов среди 134 рецептов, NBT статуй и 3 локализации (RU/BE/EN).
- Валидация runtime и пулов Polymer (headless сервер до шага EULA):
  - `python3 scripts/validate_runtime.py` — проверка 110 состояний Polymer-блоков, кэшей коллизий, структур сцен и генерации ресурс-пака.
  - `python3 scripts/validate_runtime.py --exhausted` — проверка безопасного fallback при исчерпании пулов блоков Polymer.
  - Запускаются в изолированных каталогах `build/runtime-validation-*` с `eula=false`, не повреждая рабочий мир.
- Подготовка ассетов релиза:
  - `python3 scripts/prepare_release.py` — копирует готовый JAR и sources в `release-assets/btr-world.jar` и `release-assets/btr-world-sources.jar`, формирует `SHA256SUMS.txt`.

# Генерация контента и синхронизация

- Справочник крафтов: `python3 scripts/generate_craft_guide.py` — генерирует `docs/CRAFTING.md` и обновляет блок рецептов в `README.md` напрямую из JSON. **Не редактируйте рецепты в README вручную.**
- Синхронизация имён: `python3 scripts/standardize_names.py` — единые названия сущностей, токенов и режимов в `ru_ru.json`, `be_by.json`, `en_us.json`.
- Процедурные генераторы: `scripts/generate_assets.py`, `scripts/generate_stages.py`, `scripts/generate_room_decor.py`, `scripts/generate_equipment.py`, `scripts/generate_loot_and_costumes.py`.

# Архитектура и ограничения

- Mod ID: `bocchi`, имя архивов: `btr-world`. Основной пакет: `dev.aliska.bocchi`.
- Серверный мод Polymer: клиентский мод не обязателен, визуал передаётся через автоматически собираемый Polymer resource pack (модели на базе `paper`, блоки на базе ванильных блоков).
- Ограничение атласа текстур Minecraft 1.21.1: сканируются только каталоги `assets/bocchi/textures/block/` и `assets/bocchi/textures/item/`. Файлы за их пределами (например в `textures/model/`) не попадут в ванильный атлас и приведут к missing texture на клиенте.
- Музыка и аудио: используются только оригинальные нотные фразы и ванильные звуки. Не включать защищённые авторским правом аудиозаписи или прямые транскрипции композиций из аниме.
- Modrinth: проект ориентирован на статус `unlisted` (или `draft`) с обязательным раскрытием AI-генерации (код, ассеты, текст). `scripts/modrinth_release.py` блокирует публикацию при статусе `public`.
