# FFmpeg ses decoder kurulumu (video FFmpeg değil)

Bu projede video Android/Media3'in normal video renderer'ı ile çözülür.
FFmpeg yalnızca ses decoder olarak kullanılır.

AndroidX Media3'ün resmi FFmpeg decoder modülü `FfmpegAudioRenderer` sağlar.
Modül Google Maven'de hazır AAR olarak yayınlanmıyor; AndroidX Media kaynak ağacından
ve FFmpeg native kütüphanelerinden derlenmesi gerekir.

Resmi açıklama ve derleme adımları:
https://github.com/androidx/media/blob/release/libraries/decoder_ffmpeg/README.md

Derlenmiş `lib-decoder-ffmpeg-release.aar` dosyasını:
`app/libs/` klasörüne koyun. `app/build.gradle` içindeki fileTree AAR'ı otomatik alır.
`PlayerActivity` renderer extension modunu PREFER yaptığı için FFmpeg ses renderer'ı
uygun ses formatlarında öncelikli kullanılır. Desteklenmeyen ses formatlarında
Android'in yerleşik decoder'ı fallback olabilir.

Önemli:
- FFmpeg video decoder eklenmez.
- FFmpeg AAR olmadan uygulama yine derlenir ve normal Media3 ses/video oynatımı yapar.
- AAR'ın içinde Android cihazınızın ABI'si için native `.so` dosyaları bulunmalıdır
  (arm64-v8a, armeabi-v7a, x86_64 gibi). TV Box mimarisine uygun ABI seçin.
- Resmi modül README'si FFmpeg'i ayrıca derlemeyi ve lisans yükümlülüklerini
  incelemeyi gerektirir. AAR dosyası bu ZIP'e dahil değildir; burada varmış gibi
  gösterilmiyor.
