const SMS = require('./SMS');

function readableNumber(/** @type { String } */ number) {
    return ('' + number).split('').reverse().join(' ').replace(/([^ ] [^ ] [^ ])/g, '$1 .').split('').reverse().join('');
}

module.exports = addPlugin({
    sms: {
        automatic: {
            checkNewMessage: {type: 'boolean', value: false},
        },
    },
}, {
    os: { android: true /*, ios: true*/ },
    pluginFormatVersion: 1,
}, {
    scriptInitializer: async ctx => new SMS(ctx),
    translations: /** @type { const } */ ({
        canSendMessage: {
            "sk-SK": "Môžem poslať SMS priateľovi ${realName} s textom: ${message}",
            "en-US": "Can I send a SMS to friend ${realName} with the text: ${message}"
        },
        recipientNameQuestion: {
            "sk-SK": "Komu mám správu odoslať?",
            "en-US": "Who should I send the message to?"
        },
        messageContentQuestion: {
            "sk-SK": "Môžete diktovať text správy:",
            "en-US": "You can dictate the message text:"
        },
        sendingMessage: {
            "sk-SK": "Odosielam SMS...",
            "en-US": "I am sending the SMS ..."
        },
        sendingFailed: {
            "sk-SK": "Odosielanie SMS zlihalo.",
            "en-US": "SMS sending failed."
        },
        sendingTimeout: {
            "sk-SK": "Niesom si istý, že ...",
            "en-US": "I'm not sure that ..."
        },
    }),
}, {
    moduleRequirementsFree: [{
        name: 'JJPlugin SMS apk',
        android: {
            minVersion: '1.3.2',
            packageName: 'jjplugin.obsgrass.sms',
            downloadUrl: 'https://github.com/ObscurusGrassator/jjplugin-sms/releases/download/1.3.2/JJPluginSMS_v1.3.2.apk'
        }
    }],
});
