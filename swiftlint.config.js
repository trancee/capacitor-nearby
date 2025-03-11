const config = require('@ionic/swiftlint-config');

config.excluded.push('${PWD}/nearby');

module.exports = {
  ...config,
};
