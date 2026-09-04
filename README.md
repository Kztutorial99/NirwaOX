# NirwaOS

Aplikasi Android yang meneruskan **notifikasi real-time** dari perangkat ke **bot Telegram** kamu, dengan **ID perangkat unik** (`NIR-XXX-XXX-XXX`) supaya notifikasi antar perangkat tidak tercampur. Filter per-aplikasi dikontrol langsung dari chat Telegram. APK di-build otomatis lewat GitHub Actions.

## Cara build

1. Di repo ini buka **Settings → Secrets and variables → Actions**, tambahkan:
   - `TELEGRAM_BOT_TOKEN` — token dari @BotFather
   - `TELEGRAM_CHAT_ID` — chat id tujuan (bisa didapat dari @userinfobot)
2. Jalankan workflow **Build APK** (otomatis saat push ke `main`, atau manual lewat *Run workflow*).
3. Unduh APK di tab **Actions → artifacts** atau di release `latest`.

## Pemakaian

1. Pasang APK, buka aplikasi.
2. Tekan **Izinkan Akses Notifikasi** lalu aktifkan NirwaOS di daftar.
3. Tekan **Jalankan Service** dan **Tes Kirim ke Telegram**.
4. Catat **ID Perangkat** yang tampil (contoh `NIR-4KD-9PL-2XT`). ID ini dibuat sekali saat instalasi dan ikut di setiap notifikasi yang dikirim.

## Perintah bot Telegram

Tambahkan ID perangkat di akhir perintah agar hanya perangkat itu yang merespons. Tanpa ID, semua perangkat merespons.

| Perintah | Fungsi |
| --- | --- |
| `/help` | Bantuan |
| `/ping`, `/id` | Cek perangkat online |
| `/status` | Status forwarding & perangkat |
| `/on` / `/off` | Nyalakan / matikan forwarding |
| `/apps` | Daftar aplikasi + status mute |
| `/mute <package>` | Matikan notifikasi satu app |
| `/unmute <package>` | Nyalakan lagi |
| `/muted` | Daftar app yang di-mute |
| `/muteall`, `/unmuteall` | Mute / unmute semua app |

Contoh: `/mute com.whatsapp NIR-4KD-9PL-2XT`

## Catatan

- Build release ditandatangani memakai debug keystore (untuk instalasi pribadi).
- Matikan optimasi baterai untuk NirwaOS agar service polling tetap hidup.
