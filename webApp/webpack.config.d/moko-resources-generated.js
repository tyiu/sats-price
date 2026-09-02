// noinspection JSUnnecessarySemicolon
;(function(config) {
    const path = require('path');
    const MiniCssExtractPlugin = require('mini-css-extract-plugin');

    config.module.rules.push(
        {
            test: /\.(.*)/,
            resource: [
                path.resolve(__dirname, "kotlin/assets"),
                path.resolve(__dirname, "kotlin/files"),
                path.resolve(__dirname, "kotlin/images"),
                path.resolve(__dirname, "kotlin/localization"),
            ],
            type: 'asset/resource'
        }
    );
    
    config.plugins.push(new MiniCssExtractPlugin())
    config.module.rules.push(
        {
            test: /\.css$/,
            resource: [
                path.resolve(__dirname, "kotlin/fonts"),
            ],
            use: ['style-loader', 'css-loader']
        }
    )

    config.module.rules.push(
        {
            test: /\.(otf|ttf)?$/,
            resource: [
                path.resolve(__dirname, "kotlin/fonts"),
            ],
            type: 'asset/resource',
        }
    )
})(config);