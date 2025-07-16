# BitcoinMarketRecorder
Market Data Recorder for Bitcoin on GM and Bitflyer　both future and spot

GMOおよびBitflyerのPublicのWebsocketから先物と現物の板データとトレードデータを読み込んでCSVファイルに記録します。
GMOでは、現物のBTCとレバレッジ取引のBTC_JPYの板と約定情報を記録する。
BitFlyerは、現物BTC_JPY、CFD取引がFX_BTC_JPYとなる。

またWebsocketは、GMOで１つセッションを開いてから先物・現物のマーケットボード、先物、現物の約定情報を1秒間あいだをあけてリクエストする。
Bitflyerも同様の処理をおこなう。

Java 17 SpringBoot, CSV File

## Redis Pub/Sub Integration

このアプリケーションは、Redisを利用したPub/Sub機能でリアルタイムデータ配信を行います。取得したマーケットデータを低遅延でRedisチャンネルに配信し、取引シミュレーションやリアルタイム分析に利用できます。

### TradeInsert配信

約定データ（Trade）をRedisチャンネルに配信する機能です。

- **チャンネル形式**: `trade-insert:{symbol}`
- **配信タイミング**: GMOまたはBitflyerから約定データを受信した際
- **データ形式**:
  ```json
  {
    "symbol": "G_BTCJPY",
    "price": 8500000.0,
    "quantity": 0.1,
    "side": "BUY"
  }
  ```
- **チャンネル例**:
  - `trade-insert:G_BTCJPY`
  - `trade-insert:B_FX_BTCJPY`

### MarketMake配信

板データ（MarketBoard）をRedisチャンネルに配信する機能です。

- **チャンネル形式**: `market-make:{symbol}`
- **配信タイミング**: GMOまたはBitflyerから板データを受信した際
- **データ形式**:
  ```json
  {
    "symbol": "G_BTCJPY",
    "bidLevels": [
      {"price": 8499000.0, "quantity": 0.5},
      {"price": 8498000.0, "quantity": 1.0}
    ],
    "askLevels": [
      {"price": 8501000.0, "quantity": 0.3},
      {"price": 8502000.0, "quantity": 0.8}
    ]
  }
  ```
- **チャンネル例**:
  - `market-make:G_BTCJPY`
  - `market-make:B_FX_BTCJPY`

### 設定

Redis Pub/Sub統合の設定は`application.properties`で行います：

```properties
# Redis接続設定
redis.host=localhost
redis.port=6379
redis.password=
redis.database=0

# Redis配信機能の有効/無効
redis.publisher.enabled=true

# チャンネル設定
redis.publisher.market-make.channel-prefix=market-make
redis.publisher.trade-insert.channel-prefix=trade-insert

# データ配信の有効/無効（既存設定を流用）
exch-sim.enabled=true

# シンボルマッピング設定（既存設定を流用）
# GMOのシンボルをRedisチャンネル用シンボルにマッピング
exch-sim.symbol-mapping.GMO.BTC=G_FX_BTCJPY
exch-sim.symbol-mapping.GMO.BTC_JPY=G_BTCJPY
exch-sim.symbol-mapping.GMO.ETH_JPY=G_ETHJPY

# BitflyerのシンボルをRedisチャンネル用シンボルにマッピング
exch-sim.symbol-mapping.BITFLYER.BTC_JPY=B_BTCJPY
exch-sim.symbol-mapping.BITFLYER.FX_BTC_JPY=B_FX_BTCJPY
```

### Google Cloud環境での設定

Google Cloud Memorystore for Redisを使用する場合：

```properties
# 環境変数での設定を推奨
redis.host=${REDIS_HOST:localhost}
redis.port=${REDIS_PORT:6379}
redis.password=${REDIS_PASSWORD:}
```

### データ購読方法

**Redis CLIでの確認:**
```bash
# 特定チャンネル購読
redis-cli SUBSCRIBE "market-make:G_BTCJPY"
redis-cli SUBSCRIBE "trade-insert:G_BTCJPY"

# パターンマッチ購読
redis-cli PSUBSCRIBE "market-make:*"
redis-cli PSUBSCRIBE "trade-insert:*"

# 全活動監視
redis-cli MONITOR
```

### 性能特徴

- **低遅延**: HTTP RESTと比較して1/10以下の遅延（0.1-1ms）
- **高スループット**: 非同期配信による高い処理能力
- **接続安定性**: Redis自動再接続機能
- **認証不要**: HTTPベースの認証オーバーヘッド除去

# GMO API settings
gmo.api.base-url=https://api.coin.z.com
gmo.api.ws-url=wss://api.coin.z.com/public/v1/ws


# Bitflyer API settings
bitflyer.api.base-url=https://api.bitflyer.com
bitflyer.api.ws-url=wss://ws.lightstream.bitflyer.com/json-rpc 




