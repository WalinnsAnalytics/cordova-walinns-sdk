function trackEvent(type, eventName) {
        cordova.plugins.walinnshybridapi.trackEvent(type, eventName,
            function(success){
                console.log(success);
             },
            function(failure) {
               console.log(failure);
             })
}


function trackScreen(screenName) {
        cordova.plugins.walinnshybridapi.trackScreen(screenName,
            function(success){
                console.log(success);
             },
            function(failure) {
                console.log(failure);
             })
}