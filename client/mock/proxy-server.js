const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');

const peopleApp = express();
const demographyApp = express();

peopleApp.use('/api/v1', createProxyMiddleware({
    target: 'http://localhost:51314',
    changeOrigin: true,
    pathRewrite: {
        '^/api/v1': '',
    },
    onProxyReq: (proxyReq, req, res) => {
        console.log(`Proxying ${req.method} ${req.url} -> ${proxyReq._currentUrl}`);
    },
    onProxyRes: (proxyRes, req, res) => {
        console.log(`Received response ${proxyRes.statusCode} for ${req.url}`);
    }
}));

demographyApp.use('/api/v1', createProxyMiddleware({
    target: 'http://localhost:51315',
    changeOrigin: true,
    pathRewrite: {
        '^/api/v1': '',
    },
    onProxyReq: (proxyReq, req, res) => {
        console.log(`Proxying ${req.method} ${req.url} -> ${proxyReq._currentUrl}`);
    },
    onProxyRes: (proxyRes, req, res) => {
        console.log(`Received response ${proxyRes.statusCode} for ${req.url}`);
    }
}));

peopleApp.listen(51313, () => {
    console.log('Proxy server for people-service listening on http://localhost:51313');
});

demographyApp.listen(51312, () => {
    console.log('Proxy server for demography-service listening on http://localhost:51312');
});
