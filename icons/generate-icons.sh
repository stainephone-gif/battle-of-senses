#!/bin/bash

# Скрипт для генерации placeholder иконок для PWA
# Использует ImageMagick для создания иконок из существующего изображения

echo "🎨 Генерация иконок для Battle of Senses PWA..."

# Проверка наличия ImageMagick
if ! command -v convert &> /dev/null; then
    echo "❌ ImageMagick не установлен!"
    echo "Установите его командой: sudo apt-get install imagemagick"
    echo "Или используйте онлайн-сервис: https://www.pwabuilder.com/imageGenerator"
    exit 1
fi

# Определяем путь к исходному изображению
SOURCE_IMAGE="../pic/image1.png"

if [ ! -f "$SOURCE_IMAGE" ]; then
    echo "❌ Исходное изображение не найдено: $SOURCE_IMAGE"
    exit 1
fi

echo "✅ Используем исходное изображение: $SOURCE_IMAGE"

# Размеры иконок
SIZES=(72 96 128 144 152 192 384 512)

# Создаем иконки
for SIZE in "${SIZES[@]}"; do
    OUTPUT="icon-${SIZE}.png"
    echo "   Создание $OUTPUT (${SIZE}x${SIZE}px)..."

    # Создаем иконку с черным фоном
    convert "$SOURCE_IMAGE" \
        -resize "${SIZE}x${SIZE}" \
        -gravity center \
        -background black \
        -extent "${SIZE}x${SIZE}" \
        "$OUTPUT"

    if [ $? -eq 0 ]; then
        echo "   ✅ $OUTPUT создан"
    else
        echo "   ❌ Ошибка при создании $OUTPUT"
    fi
done

echo ""
echo "🎉 Генерация иконок завершена!"
echo "📁 Иконки сохранены в: $(pwd)"
echo ""
echo "⚠️  ВАЖНО: Это временные иконки!"
echo "Для продакшена создайте профессиональные иконки с помощью:"
echo "  - https://www.pwabuilder.com/imageGenerator"
echo "  - https://realfavicongenerator.net/"
echo ""
