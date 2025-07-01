# BitcoinMarketRecorder
Market Data Recorder for Bitcoin on GM and Bitflyer　both future and spot

GMOおよびBitflyerのPublicのWebsocketから先物と現物の板データとトレードデータを読み込んでCSVファイルに記録します。
GMOでは、現物のBTCとレバレッジ取引のBTC_JPYの板と約定情報を記録する。
BitFlyerは、現物BTC_JPY、CFD取引がFX_BTC_JPYとなる。

またWebsocketは、GMOで１つセッションを開いてから先物・現物のマーケットボード、先物、現物の約定情報を1秒間あいだをあけてリクエストする。
Bitflyerも同様の処理をおこなう。

Java 17 SpringBoot, CSV File

## ExchSim Integration

このアプリケーションは、外部の取引シミュレーションシステム（ExchSim）との統合機能を提供します。取得したマーケットデータをリアルタイムでExchSimに送信し、取引シミュレーションやバックテストに利用できます。

### TradeInsert機能

約定データ（Trade）をExchSimに送信する機能です。

- **エンドポイント**: `/api/trade/insert`
- **送信タイミング**: GMOまたはBitflyerから約定データを受信した際
- **データ形式**:
  ```json
  {
    "symbol": "G_BTCJPY",
    "price": 8500000.0,
    "quantity": 0.1,
    "side": "BUY"
  }
  ```

### MarketMake機能

板データ（MarketBoard）をExchSimに送信する機能です。

- **エンドポイント**: `/api/market-make/orders`
- **送信タイミング**: GMOまたはBitflyerから板データを受信した際
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

### 設定

ExchSim統合の設定は`application.properties`で行います：

```properties
# ExchSim統合の有効/無効
exch-sim.enabled=true

# ExchSim API設定
exch-sim.api.base-url=http://localhost:8080
exch-sim.api.username=marketmaker1
exch-sim.api.password=mmpass123

# シンボルマッピング設定
# GMOのシンボルをExchSimのシンボルにマッピング
exch-sim.symbol-mapping.GMO.BTC=G_FX_BTCJPY
exch-sim.symbol-mapping.GMO.BTC_JPY=G_BTCJPY
exch-sim.symbol-mapping.GMO.ETH_JPY=G_ETHJPY

# BitflyerのシンボルをExchSimのシンボルにマッピング
exch-sim.symbol-mapping.BITFLYER.BTC_JPY=B_BTCJPY
exch-sim.symbol-mapping.BITFLYER.FX_BTC_JPY=B_FX_BTCJPY
```

### 認証

ExchSim APIへのアクセスには認証が必要です。ユーザー名とパスワードによる認証トークンを取得し、各APIリクエストにBearer認証として含めます。

### エラーハンドリング

- 401エラー（認証エラー）が発生した場合、認証トークンキャッシュをクリアして再認証を試行します
- ネットワークエラーやその他のエラーが発生した場合、ログに記録されますが、メインのデータ収集処理には影響しません

# GMO API settings
gmo.api.base-url=https://api.coin.z.com
gmo.api.ws-url=wss://api.coin.z.com/public/v1/ws


# Bitflyer API settings
bitflyer.api.base-url=https://api.bitflyer.com
bitflyer.api.ws-url=wss://ws.lightstream.bitflyer.com/json-rpc 




