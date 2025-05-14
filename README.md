# BitcoinMarketRecorder
Market Data Recorder for Bitcoin on GM and Bitflyer　both future and spot

GMOおよびBitflyerのPublicのWebsocketから先物と現物の板データとトレードデータを読み込んでCSVファイルに記録します。
GMOでは、現物のBTCとレバレッジ取引のBTC_JPYの板と約定情報を記録する。
BitFlyerは、現物BTC_JPY、CFD取引がFX_BTC_JPYとなる。

またWebsocketは、GMOで１つセッションを開いてから先物・現物のマーケットボード、先物、現物の約定情報を1秒間あいだをあけてリクエストする。
Bitflyerも同様の処理をおこなう。

Java 17 SpringBoot, CSV File

# GMO API settings
gmo.api.base-url=https://api.coin.z.com
gmo.api.ws-url=wss://api.coin.z.com/public/v1/ws


# Bitflyer API settings
bitflyer.api.base-url=https://api.bitflyer.com
bitflyer.api.ws-url=wss://ws.lightstream.bitflyer.com/json-rpc 


DBに格納するデータ形式
    CREATE TABLE IF NOT EXISTS trades (
                exchange VARCHAR,           -- 取引所（'GMO' or 'BITFLYER'）
                symbol VARCHAR,             -- 取引ペア（'BTC/JPY', 'ETH/JPY'など）
                trade_id VARCHAR,           -- 約定ID
                price DECIMAL(20,8),        -- 約定価格
                size DECIMAL(20,8),         -- 約定数量
                side VARCHAR,               -- 取引タイプ（'BUY' or 'SELL'）
                timestamp TIMESTAMP,        -- 約定時刻
                created_at TIMESTAMP        -- レコード作成時刻
      )

class MarketBoard(
    val symbol: String,
    val exchange: string,
    val bid1: Double,
    val bid2: Double,
    val bid3: Double,
    val bid4: Double,
    val bid5: Double,
    val bid6: Double,
    val bid7: Double,
    val bid8: Double,
    val ask1: Double,
    val ask2: Double,
    val ask3: Double,
    val ask4: Double,
    val ask5: Double,
    val ask6: Double,
    val ask7: Double,
    val ask8: Double,
    val bid1vol: Double,
    val bid2vol: Double,
    val bid3vol: Double,
    val bid4vol: Double,
    val bid5vol: Double,
    val bid6vol: Double,
    val bid7vol: Double,
    val bid8vol: Double,
    val ask1vol: Double,
    val ask2vol: Double,
    val ask3vol: Double,
    val ask4vol: Double,
    val ask5vol: Double,
    val ask6vol: Double,
    val ask7vol: Double,
    val ask8vol: Double,
    val ts: Instant
) 



