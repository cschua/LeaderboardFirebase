package com.leaderboardkit.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.leaderboardkit.ui.avatar.AvatarResolver
import com.leaderboardkit.ui.avatar.DefaultAvatarResolver
import com.leaderboardkit.ui.theme.AvatarShape

/**
 * Renders a locally-bundled avatar drawable for [avatarId] via [resolver] — never
 * a network fetch, since avatars are a fixed, pre-defined image set (see
 * [AvatarResolver] KDoc).
 */
@Composable
fun AvatarView(
    avatarId: String,
    shape: AvatarShape,
    modifier: Modifier = Modifier,
    resolver: AvatarResolver = DefaultAvatarResolver,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(id = resolver.resolve(avatarId)),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        // Default size applies first so a caller-supplied `modifier` (e.g. a
        // different .size()) can still override it; clip is applied last so it
        // always clips to whatever size actually won.
        modifier = Modifier.size(40.dp).then(modifier).clip(shape.toComposeShape()),
    )
}

private fun AvatarShape.toComposeShape(): Shape = when (this) {
    AvatarShape.Circle -> CircleShape
    AvatarShape.Square -> RoundedCornerShape(0.dp)
    AvatarShape.RoundedSquare -> RoundedCornerShape(8.dp)
}

@Preview(showBackground = true)
@Composable
private fun AvatarViewPreview() {
    AvatarView(avatarId = "avatar_05", shape = AvatarShape.Circle)
}
