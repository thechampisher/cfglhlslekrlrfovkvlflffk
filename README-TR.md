# İSA TV HD — Android kaynak projesi

Bu proje dearbulut API'sini kullanmaz. Kaynak adresleri doğrudan aşağıdaki GitHub depolarıdır:

- Domino: `https://github.com/dominoiptv/dominotv` — `liste.json`
- Güneş: `https://github.com/GeceHub/gunestv` — `live2.json`, `vod2.json`, `kanal_logo/`

Uygulama GitHub REST API ile deponun varsayılan dalını ve dosya ağacını çalışma anında bulur; böylece `main/master` dalını veya JSON dosyasının alt klasörünü tahmin etmez. JSON dosyalarını raw.githubusercontent.com üzerinden indirir. Her dosya JSON olarak doğrulanmadan önbelleğe alınmaz. Kullanıcı her OTOMATİK TARAMA dediğinde iki repo tekrar kontrol edilir.

## Derleme
Android Studio'da bu klasörü açın, Gradle Sync yapın ve `app` yapılandırmasını çalıştırın. Android Gradle Plugin 8.7.3, Gradle 8.9, JDK 17 ve Android SDK 35 gerekir. Android TV Box için `LEANBACK_LAUNCHER` kategorisi ve yatay arayüz vardır.

Termux'ta derlemek için ayrıca Android SDK/Build Tools/Platform 35 ve Gradle 8.9 kurulmalıdır. Sadece `pkg install openjdk` yeterli değildir.

## Şu anki kapsam / dürüst durum
- Kaynak tarama, JSON indirme, JSON ayrıştırma, yerel önbellek ve temel kanal listesi: kaynak kodda mevcut.
- Temel medya oynatma: AndroidX Media3 ExoPlayer ile mevcut.
- **FFmpeg'in hem video hem ses decoder olarak kullanılması bu sürümde mevcut değildir.** Media3 FFmpeg modülü yalnızca ses decoder entegrasyonudur; tam FFmpeg A/V için libmpv/NDK tabanlı native motor, ABI başına `.so` dosyaları ve lisans/dağıtım ayarları gerekir. Bu yüzden bu proje FFmpeg A/V tamamlandı diye sunulmaz.
- `kanal_logo/` görsellerinin indirilmesi ve eşlenmesi, gerçek JSON şemalarına özel adaptörler, EPG XMLTV, kategori/arama/favori ekranları, yayın hatası geri dönüşleri ve imzalı release APK sonraki geliştirme işidir.

## JSON şeması notu
Kaynak depoların JSON şemaları farklı olabilir. `JsonChannelParser` yaygın anahtarları (`name/title`, `url/stream/link`, `logo/icon`, `group/category`) genel olarak tanır. Depoların dosyalarının gerçek şeması farklıysa parser'a özel alan eşlemesi eklenmelidir. Kaynakta URL bulunmayan nesneler oynatılabilir kanal kabul edilmez.


## FFmpeg ses tercihi
Oynatıcı `DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER` ile kurulur.
Resmi Media3 FFmpeg audio decoder AAR'ı `app/libs/` içine eklendiğinde FFmpeg ses
renderer'ı kullanılabilir. Görüntü FFmpeg ile çözülmez; Android/Media3 video renderer
kullanılır. AAR olmadan uygulama normal Android decoder'larıyla çalışır.
Kurulum ayrıntısı: `FFMPEG-SES-KURULUM-TR.md`.
