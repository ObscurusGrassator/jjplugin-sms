var _interopRequireDefault=require("@babel/runtime/helpers/interopRequireDefault");var _asyncToGenerator2=_interopRequireDefault(require("@babel/runtime/helpers/asyncToGenerator"));try{var _require=require('react-native'),Linking=_require.Linking;}catch(err){}var lastNumbers='';function readableNumber(number){return(''+number).split('').reverse().join(' ').replace(/([^ ] [^ ] [^ ])/g,'$1 .').split('').reverse().join('');}function sendRequest(_x,_x2){return _sendRequest.apply(this,arguments);}function _sendRequest(){_sendRequest=(0,_asyncToGenerator2.default)(function*(ctx,method){var params=arguments.length>2&&arguments[2]!==undefined?arguments[2]:{};return ctx.mobileAppOpen('jjplugin.obsgrass.sms','JJPluginSMSService',[["serviceMethod",method],["input",JSON.stringify(params)]]);});return _sendRequest.apply(this,arguments);}module.exports=require("server/types/pluginFunctions.cjs").addPlugin({"sms":{"automatic":{"checkNewMessage":{"type":"boolean","value":true}}}},{"os":{"android":true},"pluginFormatVersion":1},{"moduleRequirementsFree":[{"name":"JJPlugin SMS apk","android":{"downloadUrl":"https://github.com/ObscurusGrassator/jjplugin-sms/releases/download/1.0.0/JJPluginSMS_v1.0.0.apk"}}],"scriptPerInterval":function(){var _scriptPerInterval=(0,_asyncToGenerator2.default)(function*(ctx){if(!ctx.config.sms.automatic.checkNewMessage.value)return;var newMessages=yield sendRequest(ctx,'getNewSMSs');var messages=Object.values(newMessages);var numbers=JSON.stringify(Object.keys(newMessages));if(messages&&messages.length&&lastNumbers!=numbers){lastNumbers=numbers;if(messages.length>1)return'Máš nové SMS od '+messages.map(function(a){return a.fullName||/[a-z]/i.test(a.number)&&a.number||'čísla: '+readableNumber(a.number);}).join(', ').replace(/, ([^,]+)$/,' a $1');if(messages.length===1)return'Máš novú SMS od '+(messages[0].fullName||/[a-z]/i.test(messages[0].number)&&messages[0].number||'čísla: '+readableNumber(messages[0].number));}});function scriptPerInterval(_x3){return _scriptPerInterval.apply(this,arguments);}return scriptPerInterval;}()},{"sentenceMemberRequirements":{"_or":[{"example":"Písal mi niekto?","type":"question","predicates":{"multiple":[{"verbs":[{"baseWord":/(na|od)písať/}]}]},"subjects":{"multiple":[{"origWord":/niekto/,"propName":{"niekto":"optional"}}]},"objects":[{"multiple":[{"origWord":[/správu/,/sms/],"propName":{"správu":"optional"},"attributes":[{"baseWord":/nový/,"propName":{"novú":"optional"}}]}]}]},{"example":"Prišla mi nová správa?","type":"question","predicates":{"multiple":[{"verbs":[{"baseWord":/prísť/}]}]},"subjects":{"multiple":[{"origWord":/správa/,"attributes":[{"baseWord":/nový/,"propName":{"novú":"optional"}}]}]}},{"example":"Mám nejaké nové správy?","type":"question","predicates":{"multiple":[{"verbs":[{"baseWord":/mať/}]}]},"objects":[{"multiple":[{"origWord":[/správu/,/sms/],"attributes":[{"baseWord":/nový/,"propName":{"nový":"optional"}},{"baseWord":/nejaký|dajaký/,"propName":{"nejaký":"optional"}}]}]}]}]}},function(){var _ref=(0,_asyncToGenerator2.default)(function*(ctx){var newMessages=Object.values(yield sendRequest(ctx,'getNewSMSs'));if((newMessages==null?void 0:newMessages.length)>1)return'Máš nové SMS od '+newMessages.map(function(a){return a.fullName||/[a-z]/i.test(a.number)&&a.number||'čísla: '+readableNumber(a.number);}).join(', ').replace(/, ([^,]+)$/,' a $1');if((newMessages==null?void 0:newMessages.length)===1)return'Máš novú SMS od '+(newMessages[0].fullName||/[a-z]/i.test(newMessages[0].number)&&newMessages[0].number||'čísla: '+readableNumber(newMessages[0].number));if(!newMessages||newMessages.length===0)return'Nemáš žiadne nové SMSky';});return function(_x4){return _ref.apply(this,arguments);};}(),{"sentenceMemberRequirements":{"_or":[{"example":"Prečítaj mi nové správy!","type":"command","predicates":{"multiple":[{"verbs":[{"baseWord":/prečítať/}]}]},"objects":[{"multiple":[{"origWord":[/správy/,/sms/],"attributes":[{"baseWord":/nový/,"propName":{"nový":"optional"}},{"baseWord":/všetky/,"propName":{"všetky":"optional"}}]}]}]}]}},function(){var _ref2=(0,_asyncToGenerator2.default)(function*(ctx){var newMessages=Object.values(yield sendRequest(ctx,'getNewSMSs',{setAsRread:true}));if(newMessages.length===0)return'Nemáš žiadne nové SMSky';else for(var sms of newMessages){Linking.openURL(`sms:${sms.number}`);return`${sms.fullName||/[a-z]/i.test(sms.number)&&sms.number||'Číslo: '+readableNumber(sms.number)} píše, ${sms.messages.join(', ')},`;}});return function(_x5){return _ref2.apply(this,arguments);};}(),{"sentenceMemberRequirements":{"example":"Čo mi píše <subject>?","type":"question","predicates":{"multiple":[{"verbs":[{"baseWord":[/(na|od)písať/]}]}]},"subjects":{"multiple":[{"origWord":/.*/}],"propName":{"friend":"required"}},"objects":[{"multiple":[{"origWord":/čo/}]}]}},function(){var _ref3=(0,_asyncToGenerator2.default)(function*(ctx){var contact=yield sendRequest(ctx,'getContactByName',{name:ctx.propName.friend.multiple[0].baseWord});if(!(contact!=null&&contact.fullName))return`${ctx.propName.friend.multiple[0].baseWord} som v kontaktoch nenašiel.`;var newMessages=Object.values(yield sendRequest(ctx,'getNewSMSs',{faddress:contact.number,setAsRread:true}));var sms=newMessages.find(function(sms){return sms.fullName;});if(!sms)return`${contact.fullName} ti nenapísal žiadnu novú SMS.`;else return`${contact.fullName} píše, ${sms.messages.join(', ')}`;});return function(_x6){return _ref3.apply(this,arguments);};}(),{"sentenceMemberRequirements":{"example":"Napíš správu <object> citujem ... koniec citácie!","type":"command","predicates":{"multiple":[{"verbs":[{"baseWord":[/(na|od)písať/,/(od|p)oslať/]}]}]},"objects":{"_or":[[{"multiple":[{"origWord":[/správu/,/sms(ku)?/],"propName":{"správu":"optional"},"attributes":[{"baseWord":/nový/,"propName":{"nový":"optional"}}]}]},{"multiple":[{"case":{"key":"3"}}],"propName":{"friend":"required"}}],[{"multiple":[{"origWord":[/správu/,/sms(ku)?/],"attributes":[{"baseWord":/nový/,"propName":{"nový":"optional"}}]}]},{"multiple":[{"preposition":{"origWord":"pre"}}],"propName":{"friend":"required"}}]]}}},function(){var _ref4=(0,_asyncToGenerator2.default)(function*(ctx){var friends=ctx.propName.friend.multiple.map(function(f){return f.baseWord;});var constacts=[];var unfindedNames=[];for(var friend of friends){var constact=yield sendRequest(ctx,'getContactByName',{name:friend});if(constact!=null&&constact.fullName)constacts.push(constact);else unfindedNames.push(friend);}if(unfindedNames.length){return(unfindedNames.length>1?'Mená':'Meno')+unfindedNames.join(' a ')+' som v kontaktoch nenašiel.';}else{if(!ctx.propName.citation){var _yield$ctx$speech=yield ctx.speech('Môžeš diktovať SMS',true),text=_yield$ctx$speech.text;ctx.propName.citation=text;}if(yield ctx.getSummaryAccept('FacebookChat plugin: Poslať správu '+(Object.values(constacts).length===1?'priateľovi: ':'priateľom: ')+Object.values(constacts).map(function(a){return a.fullName;}).join(', ')+' s textom: '+ctx.propName.citation)){for(var _constact of constacts){yield sendRequest(ctx,'sendSMS',{number:_constact.phoneNumbers[0].number,message:ctx.propName.citation});}return'Odoslané...';}}});return function(_x7){return _ref4.apply(this,arguments);};}());