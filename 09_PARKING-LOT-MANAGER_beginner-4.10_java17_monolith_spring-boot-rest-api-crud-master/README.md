Otomatik Garaj Bilet Sistemi
Bu proje, bir garajda otomatik bilet sistemi sağlayan bir Spring Boot uygulamasını içerir. Müşterilerin insan müdahalesi olmadan garajı kullanmasını sağlar. Bir araba garaja girdiğinde, sürücüye benzersiz bir bilet verilir. Bu bilet, aracın plakasını ve rengini belgeler ve araca uygun bir yuva atanır. Araç çıkışında bilet geri alınır ve kullanılan yuvalar serbest bırakılır.

Nasıl Kullanılır
Projeyi Başlatma:

Bu projeyi klonlayın veya ZIP olarak indirin.
Spring Boot uygulamasını başlatmak için bir IDE kullanın veya terminalde aşağıdaki komutu çalıştırın:
arduino
Copy code
mvn spring-boot:run
Garajı Kullanma:

POST /garage/park endpoint'ine bir araç ekleyin ve bilet alın.
POST /garage/leave endpoint'ine bir bilet vererek aracı garajdan çıkarın.
GET /garage/status endpoint'ini kullanarak garajdaki mevcut durumu kontrol edin.
Örnek Kullanım:

Garaja araç eklemek için:
css
Copy code
POST /garage/park
Request Body: 
{
    "plateNumber": "34-SO-1988",
    "color": "Black",
    "width": 1
}
Garajdan araç çıkarmak için:
bash
Copy code
POST /garage/leave?slotNumber=1
Garajdaki durumu kontrol etmek için:
bash
Copy code
GET /garage/status
Postman Koleksiyonu
Bu Postman koleksiyonu kullanarak servisi test edebilirsiniz.
