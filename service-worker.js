// Battle of Senses - Service Worker
// Версия 1.0.0

const CACHE_NAME = 'battle-of-senses-v1.0.0';
const RUNTIME_CACHE = 'battle-of-senses-runtime-v1';

// Файлы для кеширования при установке
const PRECACHE_URLS = [
  '/',
  '/index.html',
  '/headphones.html',
  '/sluh.html',
  '/quiet.html',
  '/zrenie_mob.html',
  '/sound-exhibition.html',
  '/manifest.json',
  // Изображения
  '/pic/image1.png',
  '/pic/image2.png',
  '/pic/Piph.png',
  '/pic/Plato.png',
  '/pic/Arist.png',
  '/pic/ranciere.jpg',
  '/pic/leibniz.jpg',
  '/pic/mcluhan.jpg',
  '/pic/kant.jpg',
  '/pic/hegel.jpg',
  '/pic/descartes.jpg',
  '/pic/aquinas.jpg'
];

// Установка Service Worker
self.addEventListener('install', (event) => {
  console.log('[Service Worker] Установка...');

  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        console.log('[Service Worker] Кеширование файлов приложения');
        return cache.addAll(PRECACHE_URLS);
      })
      .then(() => {
        console.log('[Service Worker] Все файлы закешированы');
        return self.skipWaiting(); // Активировать немедленно
      })
  );
});

// Активация Service Worker
self.addEventListener('activate', (event) => {
  console.log('[Service Worker] Активация...');

  const currentCaches = [CACHE_NAME, RUNTIME_CACHE];

  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (!currentCaches.includes(cacheName)) {
            console.log('[Service Worker] Удаление старого кеша:', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    }).then(() => {
      console.log('[Service Worker] Активирован');
      return self.clients.claim(); // Контролировать все страницы сразу
    })
  );
});

// Обработка fetch запросов
self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // Игнорировать запросы к внешним API (Philips Hue, Tuya, Google Fonts)
  if (!url.origin.includes(self.location.origin)) {
    // Для внешних ресурсов - сетевой режим с fallback
    if (url.hostname === 'fonts.googleapis.com' ||
        url.hostname === 'fonts.gstatic.com') {
      event.respondWith(
        caches.match(request).then((cachedResponse) => {
          return cachedResponse || fetch(request).then((response) => {
            return caches.open(RUNTIME_CACHE).then((cache) => {
              cache.put(request, response.clone());
              return response;
            });
          });
        })
      );
    } else {
      // Для API запросов - только сеть
      event.respondWith(fetch(request));
    }
    return;
  }

  // Для локальных ресурсов - Cache First стратегия
  event.respondWith(
    caches.match(request)
      .then((cachedResponse) => {
        if (cachedResponse) {
          console.log('[Service Worker] Из кеша:', request.url);
          return cachedResponse;
        }

        console.log('[Service Worker] Загрузка из сети:', request.url);

        return fetch(request).then((response) => {
          // Не кешировать если ответ некорректный
          if (!response || response.status !== 200 || response.type === 'error') {
            return response;
          }

          // Кешировать успешные ответы
          const responseToCache = response.clone();

          caches.open(RUNTIME_CACHE)
            .then((cache) => {
              cache.put(request, responseToCache);
            });

          return response;
        });
      })
      .catch((error) => {
        console.error('[Service Worker] Ошибка при загрузке:', request.url, error);

        // Fallback для HTML страниц - вернуть index.html
        if (request.headers.get('accept').includes('text/html')) {
          return caches.match('/index.html');
        }

        throw error;
      })
  );
});

// Обработка сообщений от клиента
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }

  if (event.data && event.data.type === 'GET_VERSION') {
    event.ports[0].postMessage({ version: CACHE_NAME });
  }
});

// Background Sync для офлайн-режима (опционально)
self.addEventListener('sync', (event) => {
  if (event.tag === 'sync-data') {
    event.waitUntil(syncData());
  }
});

async function syncData() {
  // Здесь можно добавить логику синхронизации данных
  // например, сохраненных конфигураций умного освещения
  console.log('[Service Worker] Синхронизация данных...');
}

// Периодическая синхронизация (требует разрешения)
self.addEventListener('periodicsync', (event) => {
  if (event.tag === 'update-cache') {
    event.waitUntil(updateCache());
  }
});

async function updateCache() {
  const cache = await caches.open(CACHE_NAME);
  await cache.addAll(PRECACHE_URLS);
  console.log('[Service Worker] Кеш обновлен');
}

console.log('[Service Worker] Загружен');
