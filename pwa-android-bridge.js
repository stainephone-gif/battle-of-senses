/**
 * PWA Android Bridge
 * Эмуляция Android WebAppInterface для PWA
 * Обеспечивает одинаковый функционал между нативным Android приложением и PWA
 */

(function() {
  'use strict';

  // Проверяем, не запущено ли уже в нативном приложении
  if (typeof Android !== 'undefined') {
    console.log('✅ Запущено в Android приложении');
    return;
  }

  console.log('🌐 Инициализация PWA Android Bridge');

  // ==================== TOAST NOTIFICATIONS ====================

  const toastContainer = document.createElement('div');
  toastContainer.id = 'pwa-toast-container';
  toastContainer.style.cssText = `
    position: fixed;
    top: 20px;
    left: 50%;
    transform: translateX(-50%);
    z-index: 10000;
    pointer-events: none;
  `;
  document.body.appendChild(toastContainer);

  function showToastUI(message, duration = 2000) {
    const toast = document.createElement('div');
    toast.className = 'pwa-toast';
    toast.textContent = message;
    toast.style.cssText = `
      background: rgba(0, 0, 0, 0.8);
      color: #fff;
      padding: 12px 24px;
      border-radius: 25px;
      font-size: 14px;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      margin-bottom: 10px;
      opacity: 0;
      transform: translateY(-20px);
      transition: all 0.3s ease;
      pointer-events: auto;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    `;

    toastContainer.appendChild(toast);

    // Анимация появления
    requestAnimationFrame(() => {
      toast.style.opacity = '1';
      toast.style.transform = 'translateY(0)';
    });

    // Автоматическое удаление
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(-20px)';
      setTimeout(() => toast.remove(), 300);
    }, duration);
  }

  // ==================== HEADPHONES DETECTION ====================

  let headphonesConnected = false;
  let headphonesCheckInterval = null;

  async function checkHeadphones() {
    try {
      // Используем MediaDevices API для проверки подключенных устройств
      const devices = await navigator.mediaDevices.enumerateDevices();
      const audioOutputs = devices.filter(device => device.kind === 'audiooutput');

      // Проверяем наличие не-дефолтных аудио выходов (наушники, Bluetooth и т.д.)
      const hasHeadphones = audioOutputs.some(device => {
        const label = device.label.toLowerCase();
        return label.includes('headphone') ||
               label.includes('headset') ||
               label.includes('bluetooth') ||
               label.includes('airpods') ||
               label.includes('buds') ||
               (device.deviceId !== 'default' && device.deviceId !== 'communications');
      });

      // Дополнительная проверка через Web Audio API
      const audioContext = new (window.AudioContext || window.webkitAudioContext)();
      const hasAudioContext = audioContext.state === 'running' || audioContext.state === 'suspended';

      if (hasAudioContext) {
        audioContext.close();
      }

      const nowConnected = hasHeadphones || audioOutputs.length > 1;

      // Уведомляем об изменении состояния
      if (nowConnected !== headphonesConnected) {
        headphonesConnected = nowConnected;

        if (headphonesConnected) {
          console.log('🎧 Наушники подключены');
          if (typeof window.onNativeHeadphonesConnected === 'function') {
            window.onNativeHeadphonesConnected();
          }
        } else {
          console.log('🔇 Наушники отключены');
          if (typeof window.onNativeHeadphonesDisconnected === 'function') {
            window.onNativeHeadphonesDisconnected();
          }
        }
      }

      return headphonesConnected;
    } catch (error) {
      console.warn('Ошибка проверки наушников:', error);
      return false;
    }
  }

  // Запускаем периодическую проверку наушников
  function startHeadphonesMonitoring() {
    // Первоначальная проверка
    checkHeadphones();

    // Проверяем каждые 2 секунды
    headphonesCheckInterval = setInterval(checkHeadphones, 2000);

    // Также слушаем событие devicechange
    if (navigator.mediaDevices && navigator.mediaDevices.addEventListener) {
      navigator.mediaDevices.addEventListener('devicechange', checkHeadphones);
    }
  }

  // ==================== FULLSCREEN MODE ====================

  function enableFullscreen() {
    const elem = document.documentElement;

    if (elem.requestFullscreen) {
      elem.requestFullscreen().catch(err => {
        console.warn('Не удалось включить полноэкранный режим:', err);
      });
    } else if (elem.webkitRequestFullscreen) {
      elem.webkitRequestFullscreen();
    } else if (elem.mozRequestFullScreen) {
      elem.mozRequestFullScreen();
    } else if (elem.msRequestFullscreen) {
      elem.msRequestFullscreen();
    }
  }

  function exitFullscreen() {
    if (document.exitFullscreen) {
      document.exitFullscreen();
    } else if (document.webkitExitFullscreen) {
      document.webkitExitFullscreen();
    } else if (document.mozCancelFullScreen) {
      document.mozCancelFullScreen();
    } else if (document.msExitFullscreen) {
      document.msExitFullscreen();
    }
  }

  // ==================== ANDROID API EMULATION ====================

  window.Android = {
    /**
     * Проверка, запущено ли в нативном приложении
     */
    isNativeApp() {
      return false; // PWA версия
    },

    /**
     * Получить версию "Android" (для PWA возвращаем версию браузера)
     */
    getAndroidVersion() {
      const userAgent = navigator.userAgent;
      const match = userAgent.match(/Chrome\/(\d+)/);
      return match ? match[1] : 'Unknown';
    },

    /**
     * Получить модель устройства
     */
    getDeviceModel() {
      const ua = navigator.userAgent;

      // Определяем платформу
      if (/iPhone/.test(ua)) {
        return 'Apple iPhone';
      } else if (/iPad/.test(ua)) {
        return 'Apple iPad';
      } else if (/Android/.test(ua)) {
        const model = ua.match(/Android.*;\s*([^)]+)/);
        return model ? model[1] : 'Android Device';
      } else if (/Mac/.test(ua)) {
        return 'Apple Mac';
      } else if (/Windows/.test(ua)) {
        return 'Windows PC';
      } else if (/Linux/.test(ua)) {
        return 'Linux PC';
      }

      return 'Unknown Device';
    },

    /**
     * Показать короткое Toast сообщение
     */
    showToast(message) {
      showToastUI(message, 2000);
    },

    /**
     * Показать длинное Toast сообщение
     */
    showLongToast(message) {
      showToastUI(message, 3500);
    },

    /**
     * Вибрация устройства
     */
    vibrate(milliseconds) {
      if ('vibrate' in navigator) {
        navigator.vibrate(milliseconds);
      } else {
        console.warn('Vibration API не поддерживается');
      }
    },

    /**
     * Вибрация с паттерном
     */
    vibratePattern(pattern) {
      if ('vibrate' in navigator) {
        const patternArray = pattern.split(',').map(s => parseInt(s.trim()));
        navigator.vibrate(patternArray);
      } else {
        console.warn('Vibration API не поддерживается');
      }
    },

    /**
     * Логирование
     */
    log(message) {
      console.log('[Android Bridge]', message);
    },

    /**
     * Логирование ошибок
     */
    logError(message) {
      console.error('[Android Bridge Error]', message);
    },

    /**
     * Сохранить данные (используем localStorage)
     */
    saveData(key, value) {
      try {
        localStorage.setItem(`android_${key}`, value);
        console.log(`[Android Bridge] Data saved: ${key}`);
      } catch (error) {
        console.error('[Android Bridge] Save error:', error);
      }
    },

    /**
     * Загрузить данные
     */
    loadData(key) {
      try {
        return localStorage.getItem(`android_${key}`) || '';
      } catch (error) {
        console.error('[Android Bridge] Load error:', error);
        return '';
      }
    },

    /**
     * Удалить данные
     */
    removeData(key) {
      try {
        localStorage.removeItem(`android_${key}`);
        console.log(`[Android Bridge] Data removed: ${key}`);
      } catch (error) {
        console.error('[Android Bridge] Remove error:', error);
      }
    },

    /**
     * Очистить все данные
     */
    clearAllData() {
      try {
        const keys = Object.keys(localStorage);
        keys.forEach(key => {
          if (key.startsWith('android_')) {
            localStorage.removeItem(key);
          }
        });
        console.log('[Android Bridge] All data cleared');
      } catch (error) {
        console.error('[Android Bridge] Clear error:', error);
      }
    },

    /**
     * Выход из приложения (для PWA - закрываем окно или возвращаемся)
     */
    exitApp() {
      // В PWA не можем закрыть вкладку программно
      // Возвращаемся на главную или показываем уведомление
      if (window.history.length > 1) {
        window.history.back();
      } else {
        window.location.href = 'index.html';
      }
    },

    /**
     * Включить полноэкранный режим
     */
    enterFullscreen() {
      enableFullscreen();
    },

    /**
     * Выйти из полноэкранного режима
     */
    exitFullscreen() {
      exitFullscreen();
    }
  };

  // ==================== INITIALIZATION ====================

  // Запускаем мониторинг наушников при загрузке
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', startHeadphonesMonitoring);
  } else {
    startHeadphonesMonitoring();
  }

  // Автоматически включаем полноэкранный режим при первом взаимодействии
  let fullscreenEnabled = false;
  const enableFullscreenOnce = () => {
    if (!fullscreenEnabled) {
      fullscreenEnabled = true;
      enableFullscreen();
      document.removeEventListener('click', enableFullscreenOnce);
      document.removeEventListener('touchstart', enableFullscreenOnce);
    }
  };

  // Слушаем первый клик/тап для включения полноэкранного режима
  document.addEventListener('click', enableFullscreenOnce, { once: true });
  document.addEventListener('touchstart', enableFullscreenOnce, { once: true, passive: true });

  // Обработка кнопки "Назад" через history API
  window.addEventListener('popstate', (event) => {
    console.log('[Android Bridge] Кнопка "Назад" нажата');
    // Событие обрабатывается стандартным способом браузера
  });

  // Очистка при выгрузке страницы
  window.addEventListener('beforeunload', () => {
    if (headphonesCheckInterval) {
      clearInterval(headphonesCheckInterval);
    }
    if (navigator.mediaDevices && navigator.mediaDevices.removeEventListener) {
      navigator.mediaDevices.removeEventListener('devicechange', checkHeadphones);
    }
  });

  console.log('✅ PWA Android Bridge инициализирован');

})();
