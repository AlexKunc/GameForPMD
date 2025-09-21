import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import com.example.gameforpmd.ui.game.GameViewModel

val gameModule = module {
    viewModel { GameViewModel() }
}
