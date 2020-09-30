#!/usr/bin/env bash

set -eo pipefail

rm -f crypto-material/channel.tx
rm -f crypto-material/core.yaml
rm -f crypto-material/genesis.block
rm -f crypto-material/mychannel.block
rm -rf crypto-material/crypto-config

cd docker-compose

docker-compose -f docker-compose-cli.yaml up -d
docker exec -t cli cryptogen generate --config=/etc/hyperledger/config/crypto-config.yaml --output /etc/hyperledger/config/crypto-config
docker exec -t cli configtxgen -profile ThreeOrgsOrdererGenesis -outputBlock /etc/hyperledger/config/genesis.block -channelID testchainid
docker exec -t cli configtxgen -profile ThreeOrgsChannel -outputCreateChannelTx /etc/hyperledger/config/channel.tx -channelID mychannel
docker exec -t cli configtxgen -profile ThreeOrgsChannel -outputAnchorPeersUpdate /etc/hyperledger/config/Org1MSPanchors.tx -channelID mychannel -asOrg Org1MSP
docker exec -t cli configtxgen -profile ThreeOrgsChannel -outputAnchorPeersUpdate /etc/hyperledger/config/Org2MSPanchors.tx -channelID mychannel -asOrg Org2MSP
docker exec -t cli cp /etc/hyperledger/fabric/core.yaml /etc/hyperledger/config
docker exec -t cli sh /etc/hyperledger/config/rename_sk.sh
docker-compose -f docker-compose-cli.yaml down --volumes
