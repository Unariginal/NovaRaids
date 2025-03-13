package me.unariginal.novaraids.utils

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.send.WebhookEmbed
import club.minnced.discord.webhook.send.WebhookEmbedBuilder
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import com.cobblemon.mod.common.pokemon.Pokemon
import me.unariginal.novaraids.NovaRaids
import me.unariginal.novaraids.data.Boss
import java.net.HttpURLConnection
import java.net.URI
import java.util.*


class RaidWebhookSender {
    companion object {
        private fun generateRandomColor(): Int {
            val rand = Random()
            val r = rand.nextFloat()
            val g = rand.nextFloat() / 2f
            val b = rand.nextFloat() / 2f
            return java.awt.Color(r, g, b).rgb
        }

        private fun formatPokemonName(name: String): String {
            return name.replace("cobblemon.species.", "")
                .replace('_', ' ')
                .split(' ')
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        }

        private fun getThumbnailUrl(pokemon: Pokemon): String {
            val baseUrl = "https://play.pokemonshowdown.com/sprites/%rute%/%pokemon%.gif"
            val form: String = pokemon.form.name.trim().lowercase(Locale.getDefault())


            var url = baseUrl.replace("%rute%", if (pokemon.shiny) "ani-shiny" else "ani")
            url = url.replace("%pokemon%", form)

            if (isUrlAccessible(url)) {
                return url
            }

            val fallbackUrls = listOf(
                "https://raw.githubusercontent.com/SkyNetCloud/sprites/master/sprites/pokemon/${pokemon.species.nationalPokedexNumber}.png"
            )

            for (fallbackUrl in fallbackUrls) {
                if (isUrlAccessible(fallbackUrl)) {
                    return fallbackUrl
                }
            }

            return "https://play.pokemonshowdown.com/sprites/ani/substitute.gif"
        }


        private fun isUrlAccessible(url: String): Boolean {
            return try {
                val uri = URI(url)
                val connection = uri.toURL().openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connect()
                val responseCode = connection.responseCode

                responseCode == HttpURLConnection.HTTP_OK

            } catch (e: Exception) {
                false
            }
        }

        @JvmStatic
        fun sendRaidDiscordWebhook(pokemon: Pokemon) {
            val webhook = WebhookClient.withUrl("https://ptb.discord.com/api/webhooks/")
            val randColor = generateRandomColor()
            val thumbnailUrl = getThumbnailUrl(pokemon)
            val formattedPokemonName = formatPokemonName(pokemon.species.name)

            val embed = WebhookEmbedBuilder()
                .setColor(randColor)
                .setAuthor(
                    WebhookEmbed.EmbedAuthor(
                        "$formattedPokemonName Raid Has Started",
                        "",
                        thumbnailUrl
                    )
                )
                .addField(WebhookEmbed.EmbedField(false, "In-game Command is: ", "/warp Raids"))
                .setThumbnailUrl(thumbnailUrl)
                .build()

            val messageBuilder = WebhookMessageBuilder()
                .setContent("Use What Ever Warp Command")
                .setUsername("Raid Hook")
                .setAvatarUrl("https://cdn.modrinth.com/data/MdwFAVRL/e54083a07bcd9436d1f8d2879b0d821a54588b9e.png")
                .addEmbeds(embed)

            webhook.send(messageBuilder.build())
        }

        @JvmStatic
        fun sendRaidEndDiscordWebhook(pokemon: Pokemon) {
            val webhook = WebhookClient.withUrl("https://ptb.discord.com/api/webhooks/")
            val randColor = generateRandomColor()
            val thumbnailUrl = getThumbnailUrl(pokemon)
            val formattedPokemonName = formatPokemonName(pokemon.species.name)

            //# TODO Grab DamageMap From Raid Info
//        val sortedEntries: List<Map.Entry<String, Int>> = CobblemonRaids.currentRaid.damageMap.entries
//            .sortedByDescending { it.value }

            val embed = WebhookEmbedBuilder()
                .setColor(randColor)
                .setAuthor(
                    WebhookEmbed.EmbedAuthor(
                        "$formattedPokemonName Raid Has Ended",
                        "",
                        thumbnailUrl
                    )
                )
//            .addField(WebhookEmbed.EmbedField(false, "Leaderboard:", ""))
//            .apply {
//                sortedEntries.take(10).forEachIndexed { index, entry ->
//                    addField(WebhookEmbed.EmbedField(false, "${index + 1}. ${entry.key}:", entry.value.toString()))
//                }
//            }
                .setThumbnailUrl(thumbnailUrl)
                .build()

            val messageBuilder = WebhookMessageBuilder()
                .setUsername("Raid Hook")
                .setAvatarUrl("https://cdn.modrinth.com/data/MdwFAVRL/e54083a07bcd9436d1f8d2879b0d821a54588b9e.png")
                .addEmbeds(embed)

            webhook.send(messageBuilder.build())
        }
    }
}