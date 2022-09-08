# allezon-analytics-platform

### example POST /user_tag
```json
curl -X POST http://localhost:8080/user_tag -H 'Content-Type: application/json' -d '
{ "time": "2022-03-22T12:15:00.000Z", 
  "cookie": "cookie12",
  "country": "Poland",
  "device": "PC",
  "action": "VIEW",
  "origin": "MainPage",
  "product_info": {
    "product_id" : "arimaxy",
    "brand_id": "Nike",
    "category_id": "shoes",
    "price": 330
}'
            
curl -X POST http://localhost:8080/user_tag -H 'Content-Type: application/json' -d '{
"cookie": "cookie12"
}'

```
## Notes
 - https://mimuw.rtbhouse.com/labs/1.pdf
 - https://mimuw.rtbhouse.com/OpenVPN.pdf
 - https://spring.io/guides/gs/spring-boot/

