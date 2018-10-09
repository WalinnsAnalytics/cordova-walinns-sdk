cordova.define("com.walinns.walinnshybrid.walinnshybridapi", function(require, exports, module) {
var exec = require('cordova/exec');

exports.coolMethod = function (arg0, arg1, success, error) {
    exec(success, error, 'WalinnsHybridPlugin', 'coolMethod', [arg0, arg1]);
};

exports.trackEvent = function (type, eventName, success, error) {
    exec(success, error, 'WalinnsHybridPlugin', 'trackEvent', [type, eventName]);
};

exports.trackScreen = function (screenName, success, error) {
    exec(success, error, 'WalinnsHybridPlugin', 'trackScreen', [screenName]);
};

});
