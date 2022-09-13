# allezon-analytics-platform

### example POST /user_tag
```json

curl -X POST http://localhost:8080/user_tags -H 'Content-Type: application/json' -d '
{
    "time": "2022-03-22T12:15:00.000Z",
    "cookie": "cookie123",
    "country": "Poland",
    "device": "PC",
    "action": "VIEW",
    "origin": "MainPage",
    "product_info": {
        "product_id": "arimaxy",
        "brand_id": "Nike",
        "category_id": "shoes",
        "price": 330  
    }
}'
                
curl -X POST http://localhost:8080/user_profiles/cookie123?time_range=11_22?limit=199 -H 'Content-Type: application/json' 

```
### Notes
 - https://mimuw.rtbhouse.com/labs/1.pdf
 - https://mimuw.rtbhouse.com/OpenVPN.pdf
 - https://spring.io/guides/gs/spring-boot/
 - 780e27788aa95a1f2dbcbcba04834a00
 - sudo tcpdump -ni any port 8080 -vv -s0
 - https://mimjenkins.rtb-lab.pl/login?from=%2F
 - wget -O aerospike.tgz https://artifacts.aerospike.com/aerospike-server-community/5.7.0.16/aerospike-server-community-5.7.0.16-ubuntu20.04.tgz
 - asinfo -v 'truncate-namespace:namespace=mimuw;'

