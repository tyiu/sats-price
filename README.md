# SatsPrice

This app fetches the price of Bitcoin relative to the United States Dollar from multiple sources, and converts inputted amounts between Sats, BTC, and USD.

## Notes

Precision is attempted to be kept up to 20 digits.

When NaN is displayed, it means "not a number", which can be caused by invalid input or problems fetching the price of Bitcoin from the selected source.

## Attribution

The [Bitcoin Calculator](https://www.flaticon.com/free-icons/bitcoin-calculator) icon was created by Icon home and licensed as free for personal and commercial use with attribution.

This project depends on [BigDecimal](https://github.com/mgriebling/BigDecimal), an MIT-licensed package for providing arbitrary-precision and fixed-precision decimal arithmetic in Swift.
